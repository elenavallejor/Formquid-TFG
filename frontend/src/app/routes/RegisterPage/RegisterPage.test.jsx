import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createRegistrationRequest } from '../../../api/registrationRequestsApi.js';
import { renderWithRouter } from '../../../testing/render.jsx';
import { RegisterPage } from './RegisterPage.jsx';

vi.mock('../../../api/registrationRequestsApi.js', () => ({
  createRegistrationRequest: vi.fn(),
}));

async function fillRequest(user) {
  await user.type(screen.getByRole('textbox', { name: 'Nombre completo' }), 'Ana García');
  await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), 'ANA@Example.com');
  await user.type(screen.getByRole('textbox', { name: 'Institución / Organización' }), 'Universidad');
  await user.selectOptions(screen.getByRole('combobox', { name: 'País' }), 'ES');
  await user.type(screen.getByRole('textbox', { name: 'Motivación' }), 'Quiero investigar');
  await user.type(screen.getByLabelText('Contraseña', { exact: true }), 'pass123456');
  await user.type(screen.getByLabelText('Confirmar contraseña'), 'pass123456');
}

describe('RegisterPage', () => {
  beforeEach(() => vi.resetAllMocks());

  it('impide enviar una solicitud incompleta', async () => {
    const user = userEvent.setup();
    renderWithRouter(<RegisterPage />);
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByText('Introduce tu nombre completo.')).toBeVisible();
    expect(createRegistrationRequest).not.toHaveBeenCalled();
  });

  it('envía los datos normalizados y confirma la solicitud', async () => {
    const user = userEvent.setup();
    createRegistrationRequest.mockResolvedValue({ id: 1 });
    renderWithRouter(<RegisterPage />);
    await fillRequest(user);
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('heading', { name: 'Solicitud enviada' })).toBeVisible();
    expect(createRegistrationRequest).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Ana García',
      email: 'ana@example.com',
      country: 'ES',
    }));
  });

  it('muestra el error del servidor sin perder el formulario', async () => {
    const user = userEvent.setup();
    createRegistrationRequest.mockRejectedValue(new Error('Servicio no disponible'));
    renderWithRouter(<RegisterPage />);
    await fillRequest(user);
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(screen.getByRole('textbox', { name: 'Nombre completo' })).toHaveValue('Ana García');
  });
});
