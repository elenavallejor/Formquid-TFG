import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { deleteUser, getUsers, updateUserRole } from '../../../api/usersApi.js';
import { UsersManagementPage } from './UsersManagementPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/usersApi.js', () => ({ deleteUser: vi.fn(), getUsers: vi.fn(), updateUserRole: vi.fn() }));

const managedUser = {
  id: 9,
  name: 'Ana García',
  email: 'ana@example.com',
  institution: 'Universidad',
  country: 'ES',
  role: 'EDITOR',
};

function renderPage() {
  renderWithRouter(<UsersManagementPage />, { auth: { user: { id: 1, role: 'SUPERADMIN' } } });
}

describe('UsersManagementPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getUsers.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra un resultado vacío cuando la búsqueda no encuentra usuarios', async () => {
    renderPage();
    expect(await screen.findByText('No se han encontrado usuarios que coincidan con la búsqueda.')).toBeVisible();
    expect(getUsers).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 10 }));
  });

  it('confirma el ascenso y actualiza el rol visible', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser], totalElements: 1, totalPages: 1 });
    updateUserRole.mockResolvedValue({ ...managedUser, role: 'ADMIN' });
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ASCENDER A ADMINISTRADOR' }));
    const dialog = screen.getByRole('dialog', { name: 'Ascender a administrador' });
    await user.click(within(dialog).getByRole('button', { name: 'Ascender' }));
    expect(await screen.findByRole('heading', { name: 'ANA GARCÍA - ADMINISTRADOR/A' })).toBeVisible();
    expect(updateUserRole).toHaveBeenCalledWith(9, 'ADMIN');
  });

  it('muestra el error de eliminación sin retirar al usuario', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser], totalElements: 1, totalPages: 1 });
    deleteUser.mockRejectedValue(new Error('No autorizado'));
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ELIMINAR CUENTA' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar cuenta' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar cuenta' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No autorizado');
    expect(screen.getByRole('heading', { name: 'ANA GARCÍA - EDITOR' })).toBeVisible();
  });

  it('retira permisos de administrador y conserva el usuario', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [{ ...managedUser, role: 'ADMIN' }], totalElements: 1, totalPages: 1 });
    updateUserRole.mockResolvedValue({ ...managedUser });
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ELIMINAR PERMISOS' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar permisos de administrador' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar permisos' }));
    expect(await screen.findByRole('heading', { name: 'ANA GARCÍA - EDITOR' })).toBeVisible();
    expect(updateUserRole).toHaveBeenCalledWith(9, 'EDITOR');
  });

  it.each([
    ['ADMIN', 'SUPERADMIN', 'ASCENDER A SUPERADMINISTRADOR', 'Ascender a superadministrador', 'Ascender', 'SUPERADMINISTRADOR/A'],
    ['SUPERADMIN', 'ADMIN', 'DESCENDER A ADMINISTRADOR', 'Descender a administrador', 'Descender', 'ADMINISTRADOR/A'],
  ])('cambia de %s a %s después de confirmar', async (role, nextRole, button, title, confirm, label) => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [{ ...managedUser, role }], totalElements: 1, totalPages: 1 });
    updateUserRole.mockResolvedValue({ ...managedUser, role: nextRole });
    renderPage();
    await user.click(await screen.findByRole('button', { name: button }));
    expect(updateUserRole).not.toHaveBeenCalled();
    await user.click(within(screen.getByRole('dialog', { name: title })).getByRole('button', { name: confirm }));
    expect(updateUserRole).toHaveBeenCalledWith(9, nextRole);
    expect(await screen.findByRole('heading', { name: `ANA GARCÍA - ${label}` })).toBeVisible();
  });

  it('elimina una cuenta después de confirmar', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser], totalElements: 1, totalPages: 1 });
    deleteUser.mockResolvedValue(null);
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ELIMINAR CUENTA' }));
    await user.click(within(screen.getByRole('dialog', { name: 'Eliminar cuenta' })).getByRole('button', { name: 'Eliminar cuenta' }));
    expect(await screen.findByText('No se han encontrado usuarios que coincidan con la búsqueda.')).toBeVisible();
    expect(deleteUser).toHaveBeenCalledWith(9);
  });

  it('muestra un error de carga y aplica los cambios de paginación', async () => {
    const user = userEvent.setup();
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    getUsers.mockRejectedValueOnce(new Error('Sin conexión')).mockResolvedValue({ content: [managedUser], totalElements: 25, totalPages: 3 });
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('No se han podido cargar los usuarios');
    expect(consoleError).toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: /Ordenar usuarios:/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Nombre Z-A' }));
    await waitFor(() => expect(getUsers).toHaveBeenCalledWith(expect.objectContaining({ sortOrder: 'NAME_DESC' })));
    await user.click(await screen.findByRole('button', { name: 'Página siguiente' }));
    await waitFor(() => expect(getUsers).toHaveBeenCalledWith(expect.objectContaining({ page: 1 })));
  });

  it.each([null, {}])('tolera respuestas incompletas del listado: %j', async (response) => {
    getUsers.mockResolvedValue(response);
    renderPage();
    expect(await screen.findByText('No se han encontrado usuarios que coincidan con la búsqueda.')).toBeVisible();
  });

  it('actualiza permisos aunque la respuesta no incluya el usuario y conserva otras filas', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser, { id: 10, role: 'SUPERADMIN' }], totalElements: 2, totalPages: 1 });
    updateUserRole.mockResolvedValue(null);
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ASCENDER A ADMINISTRADOR' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Ascender' }));
    expect(await screen.findByRole('heading', { name: 'ANA GARCÍA - ADMINISTRADOR/A' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'USUARIO SIN NOMBRE - SUPERADMINISTRADOR/A' })).toBeVisible();
    expect(screen.getByText('Sin institución')).toBeVisible();
  });
  it('muestra el error por defecto al cambiar permisos', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser], totalElements: 1, totalPages: 1 });
    updateUserRole.mockRejectedValue({});
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ASCENDER A ADMINISTRADOR' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Ascender' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se han podido actualizar los permisos del usuario.');
  });
  it('muestra el error por defecto al eliminar', async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ content: [managedUser], totalElements: 1, totalPages: 1 });
    deleteUser.mockRejectedValue({});
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'ELIMINAR CUENTA' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar cuenta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se ha podido eliminar el usuario.');
  });
});
