import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getAdminSurveys } from '../../../api/surveysApi.js';
import { AdminSurveysPage } from './AdminSurveysPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/surveysApi.js', () => ({ getAdminSurveys: vi.fn() }));

const survey = {
  id: 12,
  title: 'Hábitos digitales',
  lifecycleStatus: 'OPEN',
  visibility: 'LISTED',
  accessMode: 'PUBLIC',
  updatedAt: '2026-10-01T12:00:00Z',
  editorName: 'María',
};

describe('AdminSurveysPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getAdminSurveys.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra el estado vacío tras cargar la supervisión', async () => {
    renderWithRouter(<AdminSurveysPage />);
    expect(await screen.findByText('No hay encuestas que coincidan con los filtros.')).toBeVisible();
    expect(getAdminSurveys).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 9 }));
  });

  it('abre la configuración de la encuesta seleccionada', async () => {
    const user = userEvent.setup();
    getAdminSurveys.mockResolvedValue({ content: [survey], totalElements: 1, totalPages: 1 });
    renderWithRouter(
      <Routes>
        <Route path="/" element={<AdminSurveysPage />} />
        <Route path="/encuestas/:id/editar" element={<h1>Edición de encuesta</h1>} />
      </Routes>,
    );
    expect(await screen.findByRole('heading', { name: survey.title })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Supervisar' }));
    expect(screen.getByRole('heading', { name: 'Edición de encuesta' })).toBeVisible();
  });

  it.each(['PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED'])('muestra un borrador sin publicar aunque su acceso configurado sea %s', async (accessMode) => {
    getAdminSurveys.mockResolvedValue({ content: [{ ...survey, lifecycleStatus: 'DRAFT', visibility: 'UNLISTED', accessMode }], totalElements: 1, totalPages: 1 });
    renderWithRouter(<AdminSurveysPage />);

    expect(await screen.findByText('SIN PUBLICAR')).toHaveClass('table-badge-draft');
    expect(screen.getByText('BORRADOR')).toBeVisible();
    expect(screen.queryByText('PÚBLICO')).not.toBeInTheDocument();
    expect(screen.queryByText('CON CONTRASEÑA')).not.toBeInTheDocument();
    expect(screen.queryByText('USUARIOS AUTENTICADOS')).not.toBeInTheDocument();
  });

  it('informa del error de carga y permite descartarlo', async () => {
    const user = userEvent.setup();
    getAdminSurveys.mockRejectedValueOnce(new Error('Servicio no disponible'));
    renderWithRouter(<AdminSurveysPage />);
    expect(await screen.findByText('Servicio no disponible')).toBeVisible();
    await user.click(screen.getByRole('button', { name: /cerrar/i }));
    await waitFor(() => expect(screen.queryByText('Servicio no disponible')).not.toBeInTheDocument());
  });

  it('aplica la búsqueda y permite recorrer las páginas de resultados', async () => {
    const user = userEvent.setup();
    getAdminSurveys.mockResolvedValue({ content: [survey], totalElements: 20, totalPages: 3 });
    renderWithRouter(<AdminSurveysPage />);
    await screen.findByRole('heading', { name: survey.title });
    await user.click(screen.getByRole('button', { name: 'Página siguiente' }));
    await waitFor(() => expect(getAdminSurveys).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })));
    await user.type(screen.getByRole('searchbox', { name: 'Buscar encuestas supervisadas' }), '  hábitos  ');
    await waitFor(() => expect(getAdminSurveys).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, search: 'hábitos' })));
  });

  it('filtra por varios estados y visibilidades y ajusta el tamaño de página', async () => {
    const user = userEvent.setup();
    getAdminSurveys.mockResolvedValue({ content: [survey], totalElements: 20, totalPages: 3 });
    renderWithRouter(<AdminSurveysPage />);
    await screen.findByRole('heading', { name: survey.title });
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filters).getByLabelText('Seleccionar estado de la encuesta'));
    const statusOptions = within(filters).getByRole('group', { name: 'Estado de la encuesta' });
    await user.click(within(statusOptions).getByRole('checkbox', { name: 'Abiertas' }));
    await user.click(within(statusOptions).getByRole('checkbox', { name: 'Cerradas' }));
    await user.click(within(filters).getByLabelText('Seleccionar visibilidad'));
    const visibilityOptions = within(filters).getByRole('group', { name: 'Visibilidad' });
    await user.click(within(visibilityOptions).getByRole('checkbox', { name: 'Listadas' }));
    await user.click(within(visibilityOptions).getByRole('checkbox', { name: 'Ocultas' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));
    await waitFor(() => expect(getAdminSurveys).toHaveBeenCalledWith(expect.objectContaining({
      lifecycleStatuses: ['OPEN', 'CLOSED'], visibilities: ['LISTED', 'UNLISTED'], page: 0,
    })));
    await user.click(screen.getByRole('button', { name: /Encuestas por página:/ }));
    fireEvent.click(screen.getByRole('button', { name: '18 por página' }));
    await waitFor(() => expect(getAdminSurveys).toHaveBeenCalledWith(expect.objectContaining({ size: 18, page: 0 })));
  });
});
