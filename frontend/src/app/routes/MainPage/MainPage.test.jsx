import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getPublicSurveys, getRegisteredSurveys } from '../../../api/surveysApi.js';
import { renderWithRouter } from '../../../testing/render.jsx';
import { MainPage } from './MainPage.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  getPublicSurveys: vi.fn(),
  getRegisteredSurveys: vi.fn(),
}));

const emptyPage = { content: [], totalElements: 0, totalPages: 0 };
const survey = {
  id: 1,
  publicCode: 'ABCDEF234567',
  title: 'Vida digital',
  accessMode: 'PUBLIC',
};

describe('MainPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getPublicSurveys.mockResolvedValue(emptyPage);
    getRegisteredSurveys.mockResolvedValue(emptyPage);
  });

  it('muestra las secciones públicas y oculta las de cuenta a visitantes', async () => {
    renderWithRouter(<MainPage />);
    expect(screen.getByRole('heading', { name: /La tecnología/ })).toBeVisible();
    expect(await screen.findByText('No hay encuestas públicas disponibles.')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Encuestas para usuarios registrados' })).not.toBeInTheDocument();
    expect(getRegisteredSurveys).not.toHaveBeenCalled();
  });

  it('presenta una encuesta pública enlazada por su código', async () => {
    getPublicSurveys.mockImplementation(({ accessMode }) => Promise.resolve({
      content: accessMode === 'PUBLIC' ? [survey] : [],
      totalElements: accessMode === 'PUBLIC' ? 1 : 0,
      totalPages: 1,
    }));
    renderWithRouter(<MainPage />);
    const publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByRole('heading', { name: survey.title })).toBeVisible();
    expect(within(publicSection).getByRole('link', { name: `Acceder a la encuesta: ${survey.title}` })).toHaveAttribute('href', '/ABCDEF234567');
  });

  it('usa la fuente de encuestas registradas para usuarios autenticados', async () => {
    renderWithRouter(<MainPage />, { auth: { isAuthenticated: true } });
    expect(await screen.findByRole('heading', { name: 'Encuestas para usuarios registrados' })).toBeVisible();
    expect(getRegisteredSurveys).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'AUTHENTICATED' }));
    expect(getPublicSurveys).not.toHaveBeenCalled();
  });

  it('permite reintentar una sección que falla al cargar', async () => {
    const user = userEvent.setup();
    getPublicSurveys.mockImplementation(({ accessMode }) => {
      if (accessMode !== 'PUBLIC') return Promise.resolve(emptyPage);
      return getPublicSurveys.mock.calls.filter(([options]) => options.accessMode === 'PUBLIC').length === 1
        ? Promise.reject(new Error('Sin red'))
        : Promise.resolve({ content: [survey], totalElements: 1, totalPages: 1 });
    });
    renderWithRouter(<MainPage />);
    const publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByRole('alert')).toHaveTextContent('No hemos podido conectar');
    await user.click(within(publicSection).getByRole('button', { name: 'Reintentar' }));
    expect(await within(publicSection).findByRole('heading', { name: survey.title })).toBeVisible();
  });
});
