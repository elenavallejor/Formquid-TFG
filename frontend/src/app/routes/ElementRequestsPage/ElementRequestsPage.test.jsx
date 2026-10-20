import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { approveElementRequest, getPendingElementRequests, rejectElementRequest } from '../../../api/elementRequestsApi.js';
import { ElementRequestsPage } from './ElementRequestsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/elementRequestsApi.js', () => ({
  approveElementRequest: vi.fn(),
  getPendingElementRequests: vi.fn(),
  rejectElementRequest: vi.fn(),
}));
vi.mock('../../../components/Question/Question.jsx', () => ({
  Question: ({ data }) => <p>Vista previa: {data.statement}</p>,
}));

const request = {
  id: 5,
  elementType: 'FIXED_QUESTION',
  requestPayload: { question: { statement: '¿Qué opinas?', questionType: 'TEXT' } },
  requestedByName: 'Ana García',
  requestedByRole: 'EDITOR',
  requestedByEmail: 'ana@example.com',
  requestedByCountry: 'ES',
  message: 'Pregunta necesaria',
  createdAt: '2026-10-01T12:00:00Z',
};

describe('ElementRequestsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getPendingElementRequests.mockResolvedValue([]);
  });

  it('muestra el estado vacío sin solicitudes pendientes', async () => {
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByRole('heading', { name: 'No hay solicitudes pendientes' })).toBeVisible();
  });

  it('filtra las solicitudes y confirma la aprobación', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([request]);
    approveElementRequest.mockResolvedValue(null);
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Vista previa: ¿Qué opinas?')).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: /Introduce una palabra/ }), 'sin coincidencias');
    expect(screen.getByRole('heading', { name: 'No hay coincidencias' })).toBeVisible();
    await user.clear(screen.getByRole('searchbox', { name: /Introduce una palabra/ }));
    await user.click(screen.getByRole('button', { name: 'Aceptar' }));
    const dialog = screen.getByRole('dialog', { name: 'Aceptar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Aceptar' }));
    expect(approveElementRequest).toHaveBeenCalledWith(5);
    expect(await screen.findByRole('heading', { name: 'No hay solicitudes pendientes' })).toBeVisible();
  });

  it('mantiene visible la propuesta si falla el rechazo', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([request]);
    rejectElementRequest.mockRejectedValue(new Error('Sin red'));
    renderWithRouter(<ElementRequestsPage />);
    await user.click(await screen.findByRole('button', { name: 'Denegar' }));
    const dialog = screen.getByRole('dialog', { name: 'Denegar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Denegar' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se ha podido resolver la solicitud.');
    expect(screen.getByText('¿Qué opinas?')).toBeVisible();
  });

  it('muestra y contrae la estructura de un bloque solicitado', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([{ ...request, id: 6, elementType: 'BLOCK', requestPayload: {
      name: 'Hábitos', items: [{ itemType: 'TEXT', textBody: 'Introducción' }, { itemType: 'QUESTION', fixedQuestionId: 9 }],
    } }]);
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Introducción')).toBeVisible();
    expect(screen.getByText('Vista previa: Pregunta del catálogo #9')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ocultar estructura' }));
    expect(screen.queryByText('Introducción')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Mostrar estructura' }));
    expect(screen.getByText('Introducción')).toBeVisible();
  });

  it('muestra referencias de la plantilla y el error de carga', async () => {
    getPendingElementRequests.mockResolvedValue([{ ...request, id: 7, elementType: 'TEMPLATE', requestPayload: {
      title: 'Modelo', items: [{ itemType: 'TEXT', textBody: 'Contexto' }, { itemType: 'BLOCK', blockId: 8 }],
    } }]);
    const view = renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Contexto')).toBeVisible();
    expect(screen.getByText('A. Bloque del catálogo #8')).toBeVisible();
    view.unmount();
    getPendingElementRequests.mockRejectedValue(new Error('Red'));
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron obtener las solicitudes del catálogo.');
  });
});
