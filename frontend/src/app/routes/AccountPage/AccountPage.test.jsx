import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { deleteMyAccount, getMe, removeMyProfilePhoto, updateMyAccount, uploadMyProfilePhoto } from '../../../api/usersApi.js';
import { AccountPage } from './AccountPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/usersApi.js', () => ({ deleteMyAccount: vi.fn(), getMe: vi.fn(), removeMyProfilePhoto: vi.fn(), updateMyAccount: vi.fn(), uploadMyProfilePhoto: vi.fn() }));

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

  it('permite subir y quitar una foto y actualiza la sesión', async () => {
    const user = userEvent.setup();
    const setAuthenticatedUser = vi.fn();
    const imageKey = '/api/v1/users/7/photo/123e4567-e89b-12d3-a456-426614174000.png';
    uploadMyProfilePhoto.mockResolvedValue({ ...account, profileImageKey: imageKey, role: 'EDITOR' });
    removeMyProfilePhoto.mockResolvedValue({ ...account, profileImageKey: null, role: 'EDITOR' });
    renderPage({ setAuthenticatedUser });
    await screen.findByRole('textbox', { name: 'Nombre' });

    const file = new File(['imagen'], 'perfil.png', { type: 'image/png' });
    await user.upload(screen.getByLabelText('Seleccionar foto de perfil'), file);
    await user.click(screen.getByRole('button', { name: 'Guardar foto' }));
    expect(uploadMyProfilePhoto).toHaveBeenCalledWith(file);
    expect(await screen.findByRole('button', { name: 'Quitar foto' })).toBeVisible();
    expect(setAuthenticatedUser).toHaveBeenCalledWith(expect.objectContaining({ profileImageKey: imageKey }));

    await user.click(screen.getByRole('button', { name: 'Quitar foto' }));
    expect(removeMyProfilePhoto).toHaveBeenCalledOnce();
    expect(await screen.findByRole('status')).toHaveTextContent('Foto de perfil eliminada.');
    expect(setAuthenticatedUser).toHaveBeenLastCalledWith(expect.objectContaining({ profileImageKey: null }));
  });

  it('guarda una foto seleccionada desde el botón general de cambios', async () => {
    const user = userEvent.setup();
    uploadMyProfilePhoto.mockResolvedValue({ ...account, role: 'EDITOR', profileImageKey: '/api/v1/users/7/photo/nueva.png' });
    renderPage();
    await screen.findByRole('textbox', { name: 'Nombre' });
    await user.upload(screen.getByLabelText('Seleccionar foto de perfil'), new File(['imagen'], 'perfil.png', { type: 'image/png' }));

    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));

    expect(uploadMyProfilePhoto).toHaveBeenCalledOnce();
    expect(updateMyAccount).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog', { name: 'Confirmar contraseña' })).not.toBeInTheDocument();
  });

  it('guarda los datos y la foto juntos desde el formulario', async () => {
    const user = userEvent.setup();
    const setAuthenticatedUser = vi.fn();
    const key = '/api/v1/users/7/photo/nueva.png';
    updateMyAccount.mockResolvedValue({ ...account, name: 'Ana López', role: 'EDITOR' });
    uploadMyProfilePhoto.mockResolvedValue({ ...account, name: 'Ana López', role: 'EDITOR', profileImageKey: key });
    renderPage({ setAuthenticatedUser });
    const name = await screen.findByRole('textbox', { name: 'Nombre' });
    await user.clear(name);
    await user.type(name, 'Ana López');
    const file = new File(['imagen'], 'perfil.png', { type: 'image/png' });
    await user.upload(screen.getByLabelText('Seleccionar foto de perfil'), file);

    await user.click(screen.getByRole('button', { name: 'GUARDAR CAMBIOS' }));
    const dialog = screen.getByRole('dialog', { name: 'Confirmar contraseña' });
    fireEvent.change(within(dialog).getByLabelText('Contraseña actual'), { target: { value: 'clave' } });
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar' }));

    expect(updateMyAccount).toHaveBeenCalledOnce();
    expect(uploadMyProfilePhoto).toHaveBeenCalledWith(file);
    expect(setAuthenticatedUser).toHaveBeenLastCalledWith(expect.objectContaining({ name: 'Ana López', profileImageKey: key }));
  });

  it('rechaza un archivo de perfil que no sea imagen', async () => {
    const user = userEvent.setup({ applyAccept: false });
    renderPage();
    await screen.findByRole('textbox', { name: 'Nombre' });
    await user.upload(screen.getByLabelText('Seleccionar foto de perfil'), new File(['texto'], 'perfil.txt', { type: 'text/plain' }));
    expect(screen.getByRole('alert')).toHaveTextContent('PNG o JPEG');
    expect(screen.queryByRole('button', { name: 'Guardar foto' })).not.toBeInTheDocument();
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
