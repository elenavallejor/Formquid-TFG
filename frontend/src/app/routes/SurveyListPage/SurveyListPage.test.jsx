import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createCollaborationRequest } from '../../../api/collaborationsApi.js';
import { getSurveyDirectory } from '../../../api/surveysApi.js';
import { renderWithRouter } from '../../../testing/render.jsx';
import { SurveyListPage } from './SurveyListPage.jsx';

vi.mock('../../../api/surveysApi.js', () => ({ getSurveyDirectory: vi.fn() }));
vi.mock('../../../api/collaborationsApi.js', () => ({ createCollaborationRequest: vi.fn() }));

const survey = {
  id: 12,
  title: 'Hábitos digitales',
  lifecycleStatus: 'OPEN',
  updatedAt: '2026-10-01T12:00:00Z',
  editorName: 'María',
};

function renderList() {
  renderWithRouter(<SurveyListPage />, { auth: { user: { id: 7, role: 'EDITOR' } } });
}

describe('SurveyListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getSurveyDirectory.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra el estado vacío cuando no hay encuestas', async () => {
    renderList();
    expect(await screen.findByText('No hay encuestas que coincidan con los filtros.')).toBeVisible();
    expect(getSurveyDirectory).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 9 }));
  });

  it('permite solicitar colaboración y actualiza la tarjeta', async () => {
    const user = userEvent.setup();
    getSurveyDirectory.mockResolvedValue({ content: [survey], totalElements: 1, totalPages: 1 });
    createCollaborationRequest.mockResolvedValue({ id: 5 });
    renderList();
    expect(await screen.findByRole('heading', { name: survey.title })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Pedir colaborar' }));
    expect(await screen.findByText('Solicitud de colaboración enviada al editor responsable.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Solicitud pendiente' })).toBeDisabled();
    expect(createCollaborationRequest).toHaveBeenCalledWith(12, expect.objectContaining({ userId: 7 }));
  });

  it('expone un error de carga y permite cambiar la búsqueda', async () => {
    const user = userEvent.setup();
    getSurveyDirectory.mockRejectedValueOnce(new Error('Servicio no disponible'));
    renderList();
    expect(await screen.findByText('Servicio no disponible')).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: 'Buscar encuestas' }), 'salud');
    expect(screen.getByRole('searchbox', { name: 'Buscar encuestas' })).toHaveValue('salud');
  });

  it('mantiene la tarjeta y comunica un fallo al solicitar colaboración', async () => {
    const user = userEvent.setup();
    getSurveyDirectory.mockResolvedValue({ content: [survey], totalElements: 1, totalPages: 1 });
    createCollaborationRequest.mockRejectedValue(new Error('Solicitud duplicada'));
    renderList();
    await user.click(await screen.findByRole('button', { name: 'Pedir colaborar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Solicitud duplicada');
    expect(screen.getByRole('heading', { name: survey.title })).toBeVisible();
  });

  it('aplica filtros, ordenación y tamaño de página a la consulta', async () => {
    const user = userEvent.setup();
    getSurveyDirectory.mockResolvedValue({ content: [survey], totalElements: 20, totalPages: 3 });
    renderList();
    await screen.findByRole('heading', { name: survey.title });
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filters).getByRole('radio', { name: 'Abiertas' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));
    await waitFor(() => expect(getSurveyDirectory).toHaveBeenCalledWith(expect.objectContaining({ lifecycleStatus: 'OPEN' })));
    await user.click(screen.getByRole('button', { name: /Ordenar encuestas:/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Más antiguas primero' }));
    await waitFor(() => expect(getSurveyDirectory).toHaveBeenCalledWith(expect.objectContaining({ sortOrder: 'OLDEST' })));
    await user.click(screen.getByRole('button', { name: /Encuestas por página:/ }));
    fireEvent.click(screen.getByRole('button', { name: '18 por página' }));
    await waitFor(() => expect(getSurveyDirectory).toHaveBeenCalledWith(expect.objectContaining({ size: 18, page: 0 })));
  });
});
