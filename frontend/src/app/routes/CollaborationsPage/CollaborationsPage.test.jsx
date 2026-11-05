import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { acceptCollaborationRequest, getCollaborationOverview, rejectCollaborationRequest } from '../../../api/collaborationsApi.js';
import { CollaborationsPage } from './CollaborationsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/collaborationsApi.js', () => ({
  acceptCollaborationRequest: vi.fn(),
  getCollaborationOverview: vi.fn(),
  rejectCollaborationRequest: vi.fn(),
}));

const request = {
  id: 3,
  surveyId: 12,
  surveyTitle: 'Hábitos digitales',
  requestType: 'ACCESS_REQUEST',
  ownerId: 7,
  userId: 9,
  userName: 'Ana García',
  userEmail: 'ana@example.com',
  permission: 'EDIT',
  createdAt: '2026-10-01T12:00:00Z',
};

function renderPage() {
  renderWithRouter(<CollaborationsPage />, { auth: { user: { id: 7 } } });
}

describe('CollaborationsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCollaborationOverview.mockResolvedValue([]);
  });

  it('muestra las secciones vacías después de cargar', async () => {
    renderPage();
    expect(await screen.findByText('No has recibido solicitudes de colaboración pendientes.')).toBeVisible();
    expect(screen.getByText('No has recibido invitaciones pendientes.')).toBeVisible();
  });

  it('acepta una solicitud recibida y la retira de la tabla', async () => {
    const user = userEvent.setup();
    getCollaborationOverview.mockResolvedValue([request]);
    acceptCollaborationRequest.mockResolvedValue(null);
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Aceptar solicitud de Ana García para Hábitos digitales' }));
    expect(await screen.findByText('Solicitud aceptada.')).toBeVisible();
    expect(acceptCollaborationRequest).toHaveBeenCalledWith(12, 3);
    expect(screen.getByText('No has recibido solicitudes de colaboración pendientes.')).toBeVisible();
  });

  it('mantiene la solicitud tras un rechazo fallido', async () => {
    const user = userEvent.setup();
    getCollaborationOverview.mockResolvedValue([request]);
    rejectCollaborationRequest.mockRejectedValue(new Error('Red no disponible'));
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Rechazar solicitud de Ana García para Hábitos digitales' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Rechazar solicitud' }));
    expect(await screen.findByText('Red no disponible')).toBeVisible();
    expect(rejectCollaborationRequest).toHaveBeenCalledWith(12, 3);
    expect(screen.getByRole('button', { name: 'Rechazar solicitud de Ana García para Hábitos digitales' })).toBeVisible();
  });
});


describe('CollaborationsPage: invitaciones y solicitudes enviadas', () => {
  it.each(['ACCESS_REQUEST', 'INVITATION'])('agrupa entradas enviadas de tipo %s con y sin metadatos', async (requestType) => {
    getCollaborationOverview.mockResolvedValue([
      { ...request, requestType, ownerId: requestType === 'INVITATION' ? 7 : 8, userId: requestType === 'INVITATION' ? 8 : 7, message: 'Mensaje', ownerName: 'Propietaria', ownerEmail: 'owner@example.com' },
      { id: 4, requestType, ownerId: requestType === 'INVITATION' ? 7 : 8, userId: requestType === 'INVITATION' ? 8 : 7, surveyTitle: 'Otra encuesta', permission: 'CUSTOM' },
    ]);
    renderPage();
    expect(await screen.findByText('Otra encuesta')).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Mensaje' })).toBeVisible();
    expect(screen.getByText('CUSTOM')).toBeVisible();
    expect(screen.getAllByText('Pendiente')).toHaveLength(2);
  });
  it.each([true, false])('resuelve una invitación recibida: aceptar=%s', async (accept) => {
    const user = userEvent.setup();
    getCollaborationOverview.mockResolvedValue([{ ...request, requestType: 'INVITATION', ownerId: 8, userId: 7, ownerName: 'Marta', ownerEmail: 'm@example.com', permission: 'VIEW' }]);
    acceptCollaborationRequest.mockResolvedValue(null);
    rejectCollaborationRequest.mockResolvedValue(null);
    renderPage();
    await user.click(await screen.findByRole('button', { name: `${accept ? 'Aceptar' : 'Rechazar'} invitación de Marta para Hábitos digitales` }));
    if (!accept) await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rechazar invitación' }));
    expect(await screen.findByText(`Invitación ${accept ? 'aceptada' : 'rechazada'}.`)).toBeVisible();
    expect(accept ? acceptCollaborationRequest : rejectCollaborationRequest).toHaveBeenCalledWith(12, 3);
  });
  it('presenta un error de carga sin mensaje del servidor', async () => {
    getCollaborationOverview.mockRejectedValue({});
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('No se han podido cargar las colaboraciones.');
  });
});
