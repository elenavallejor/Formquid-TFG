import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToStaticMarkup } from 'react-dom/server';
import { BsPeople } from 'react-icons/bs';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { getCategories } from '../../api/categoriesApi.js';
import { SurveyCard } from './SurveyCard.jsx';

vi.mock('../../api/categoriesApi.js', () => ({ getCategories: vi.fn() }));

const survey = { id: 4, title: 'Hábitos', lifecycleStatus: 'OPEN', accessMode: 'PUBLIC', visibility: 'LISTED', editorName: 'Ana', responses: 2 };

function renderCard(props = {}) {
  return render(<MemoryRouter><SurveyCard survey={survey} {...props} /></MemoryRouter>);
}

describe('SurveyCard', () => {
  it.each(['editor', 'admin', 'directory', 'results', 'public'])(
    'muestra el nombre y el icono de la categoría en la tarjeta %s',
    async (variant) => {
      getCategories.mockResolvedValue([{ code: 'SALUD', name: 'Salud y bienestar', icon: 'BsPeople' }]);
      const { container } = renderCard({ variant, survey: { ...survey, category: 'SALUD' } });

      expect(await screen.findByText('Salud y bienestar')).toBeVisible();
      expect(container.querySelector('.survey-card-category svg').outerHTML)
        .toBe(renderToStaticMarkup(<BsPeople aria-hidden="true" />));
      expect(getCategories).toHaveBeenCalledWith({ surveyOnly: true });
    },
  );

  it('comparte la consulta de categorías entre tarjetas simultáneas', async () => {
    getCategories.mockClear();
    getCategories.mockResolvedValue([{ code: 'SALUD', name: 'Salud', icon: 'BsPeople' }]);
    render(<MemoryRouter>
      <SurveyCard survey={{ ...survey, id: 4, category: 'SALUD' }} />
      <SurveyCard survey={{ ...survey, id: 5, category: 'SALUD' }} />
    </MemoryRouter>);

    expect(await screen.findAllByText('Salud')).toHaveLength(2);
    expect(getCategories).toHaveBeenCalledOnce();
  });

  it('permite las acciones de edición y consulta de una encuesta propia', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    const onView = vi.fn();
    const onMoreOptions = vi.fn();
    renderCard({ onEdit, onView, onMoreOptions });
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Ver' }));
    await user.click(screen.getByRole('button', { name: 'Más opciones para Hábitos' }));
    expect(onEdit).toHaveBeenCalledWith(4);
    expect(onView).toHaveBeenCalledWith(4);
    expect(onMoreOptions).toHaveBeenCalledWith(4);
  });

  it('distingue la supervisión y muestra los metadatos', async () => {
    const user = userEvent.setup();
    const onSupervise = vi.fn();
    renderCard({ variant: 'admin', onSupervise });
    expect(screen.getByText('LISTADA')).toBeVisible();
    expect(screen.getByText('PÚBLICO')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Supervisar' }));
    expect(onSupervise).toHaveBeenCalledWith(4);
  });

  it.each([
    ['PUBLIC', 'PÚBLICO'],
    ['PASSWORD_PROTECTED', 'CON CONTRASEÑA'],
    ['AUTHENTICATED', 'USUARIOS AUTENTICADOS'],
  ])('muestra el acceso %s cuando la encuesta está publicada', (accessMode, label) => {
    renderCard({ variant: 'admin', survey: { ...survey, accessMode } });
    expect(screen.getByText(label)).toBeVisible();
    expect(screen.queryByText('SIN PUBLICAR')).not.toBeInTheDocument();
  });

  it.each([
    ['ownedByCurrentUser', 'Tu encuesta'],
    ['collaborating', 'Ya colaboras'],
    ['collaborationRequestPending', 'Solicitud pendiente'],
  ])('bloquea la solicitud cuando %s', (state, label) => {
    renderCard({ variant: 'directory', survey: { ...survey, [state]: true } });
    expect(screen.getByRole('button', { name: label })).toBeDisabled();
  });

  it('enlaza una encuesta pública con acceso protegido', () => {
    renderCard({ variant: 'public', survey: { ...survey, accessMode: 'PASSWORD_PROTECTED' }, accessHref: '/ABC123DEF456' });
    expect(screen.getByText('Acceso con contraseña').tagName).toBe('STRONG');
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Acceder a la encuesta: Hábitos' })).toHaveAttribute('href', '/ABC123DEF456');
  });
});
