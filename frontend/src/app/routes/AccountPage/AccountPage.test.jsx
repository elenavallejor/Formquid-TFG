import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { deleteMyAccount, getMe, updateMyAccount } from '../../../api/usersApi.js';
import { AccountPage } from './AccountPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/usersApi.js', () => ({ deleteMyAccount: vi.fn(), getMe: vi.fn(), updateMyAccount: vi.fn() }));

const account = {
  id: 7,
  name: 'Ana García',
  institution: 'Universidad',
  country: 'ES',
  email: 'ana@example.com',
  hasOwnedSurveys: false,
};

function renderPage(auth = {}) {
  renderWithRouter(<AccountPage />, { auth });
}

describe('AccountPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getMe.mockResolvedValue(account);
  });

  it('carga la cuenta y evita guardar si no hay cambios', async () => {
    const user = userEvent.setup();
    renderPage();
    expect(await screen.findByRole('textbox', { name: 'Nombre' })).toHaveValue('Ana García');
    expect(screen.getByRole('textbox', { name: 'Correo actual' })).toHaveValue('ana@example.com');
    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    expect(screen.getByRole('alert')).toHaveTextContent('No se ha detectado ningún cambio para guardar.');
    expect(updateMyAccount).not.toHaveBeenCalled();
  });

  it('confirma la contraseña y guarda los cambios personales', async () => {
    const user = userEvent.setup();
    const setAuthenticatedUser = vi.fn();
    updateMyAccount.mockResolvedValue({ ...account, name: 'Ana López' });
    renderPage({ setAuthenticatedUser });
    const name = await screen.findByRole('textbox', { name: 'Nombre' });
    await user.clear(name);
    await user.type(name, 'Ana López');
    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    const dialog = screen.getByRole('dialog', { name: 'Confirmar contraseña' });
    fireEvent.change(within(dialog).getByLabelText('Contraseña actual'), { target: { value: 'clave' } });
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Los cambios se han guardado correctamente.');
    expect(updateMyAccount).toHaveBeenCalledWith(expect.objectContaining({
      currentPassword: 'clave', profile: { name: 'Ana López', institution: 'Universidad', country: 'ES' },
    }));
    expect(setAuthenticatedUser).toHaveBeenCalledWith(expect.objectContaining({ name: 'Ana López' }));
  });

  it('bloquea la baja cuando hay encuestas propias', async () => {
    getMe.mockResolvedValue({ ...account, hasOwnedSurveys: true });
    renderPage();
    expect(await screen.findByRole('heading', { name: 'No puedes solicitar la baja porque tienes encuestas propias.' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Solicitar baja de cuenta' })).toBeDisabled();
  });

  it('solicita la baja y borra la sesión tras confirmar', async () => {
    const user = userEvent.setup();
    const clearAuthentication = vi.fn();
    deleteMyAccount.mockResolvedValue(null);
    renderPage({ clearAuthentication });
    await screen.findByRole('heading', { name: 'MI CUENTA' });
    await user.click(screen.getByRole('button', { name: 'Solicitar baja de cuenta' }));
    const dialog = screen.getByRole('dialog', { name: 'Confirmar contraseña' });
    fireEvent.change(within(dialog).getByLabelText('Contraseña actual'), { target: { value: 'clave' } });
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar' }));
    expect(deleteMyAccount).toHaveBeenCalledWith({ currentPassword: 'clave' });
    expect(clearAuthentication).toHaveBeenCalledOnce();
  });

  it('valida contraseñas y correos antes de pedir la contraseña actual', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole('textbox', { name: 'Nombre' });
    await user.type(screen.getByLabelText('Nueva contraseña'), 'corta');
    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    expect(screen.getByRole('alert')).toHaveTextContent('al menos 8 caracteres');
    await user.clear(screen.getByLabelText('Nueva contraseña'));
    await user.type(screen.getByRole('textbox', { name: 'Nuevo correo' }), 'ana@example.com');
    await user.type(screen.getByRole('textbox', { name: 'Confirmar nuevo correo' }), 'ana@example.com');
    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    expect(screen.getByRole('alert')).toHaveTextContent('distinto al actual');
    expect(screen.queryByRole('dialog', { name: 'Confirmar contraseña' })).not.toBeInTheDocument();
  });

  it('conserva el formulario y muestra un error si falla la actualización', async () => {
    const user = userEvent.setup();
    updateMyAccount.mockRejectedValue(new Error('Contraseña incorrecta'));
    renderPage();
    const name = await screen.findByRole('textbox', { name: 'Nombre' });
    await user.clear(name);
    await user.type(name, 'Ana López');
    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    const dialog = screen.getByRole('dialog', { name: 'Confirmar contraseña' });
    fireEvent.change(within(dialog).getByLabelText('Contraseña actual'), { target: { value: 'erronea' } });
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Contraseña incorrecta');
    expect(name).toHaveValue('Ana López');
  });

  it('informa cuando no se pueden cargar los datos de la cuenta', async () => {
    getMe.mockRejectedValue(new Error('Sin conexión'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Sin conexión');
  });
});
