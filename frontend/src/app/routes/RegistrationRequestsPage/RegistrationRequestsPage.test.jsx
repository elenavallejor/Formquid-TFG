import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { approveRegistrationRequest, getPendingRegistrationRequests, rejectRegistrationRequest } from '../../../api/registrationRequestsApi.js';
import { renderWithRouter } from '../../../testing/render.jsx';
import { RegistrationRequestsPage } from './RegistrationRequestsPage.jsx';

vi.mock('../../../api/registrationRequestsApi.js', () => ({
  approveRegistrationRequest: vi.fn(),
  getPendingRegistrationRequests: vi.fn(),
  rejectRegistrationRequest: vi.fn(),
}));

const request = {
  id: 5,
  name: 'Ana García',
  email: 'ana@example.com',
  institution: 'Universidad',
  country: 'ES',
  motivation: 'Investigar',
  createdAt: '2026-10-01T12:00:00Z',
};

describe('RegistrationRequestsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getPendingRegistrationRequests.mockResolvedValue([request]);
  });

  it('muestra y filtra las solicitudes pendientes', async () => {
    const user = userEvent.setup();
    renderWithRouter(<RegistrationRequestsPage />);
    expect(await screen.findByRole('heading', { name: request.name })).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: /Introduce una palabra/ }), 'desconocido');
    expect(screen.getByText('No se han encontrado solicitudes de registro pendientes.')).toBeVisible();
  });

  it('confirma la aprobación y retira la solicitud', async () => {
    const user = userEvent.setup();
    approveRegistrationRequest.mockResolvedValue(null);
    renderWithRouter(<RegistrationRequestsPage />);
    await screen.findByRole('heading', { name: request.name });
    await user.click(screen.getByRole('button', { name: `Aceptar solicitud de ${request.name}` }));
    const dialog = screen.getByRole('dialog', { name: 'Aceptar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Aceptar' }));
    expect(await screen.findByText('No se han encontrado solicitudes de registro pendientes.')).toBeVisible();
    expect(approveRegistrationRequest).toHaveBeenCalledWith(5);
  });

  it('mantiene abierto el diálogo cuando el rechazo falla', async () => {
    const user = userEvent.setup();
    rejectRegistrationRequest.mockRejectedValue(new Error('Red no disponible'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    renderWithRouter(<RegistrationRequestsPage />);
    await screen.findByRole('heading', { name: request.name });
    await user.click(screen.getByRole('button', { name: `Denegar solicitud de ${request.name}` }));
    const dialog = screen.getByRole('dialog', { name: 'Rechazar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Rechazar' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se ha podido procesar la solicitud');
    expect(screen.getByRole('heading', { name: request.name })).toBeVisible();
  });
});
