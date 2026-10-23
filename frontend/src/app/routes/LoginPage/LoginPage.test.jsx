import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LuEye, LuEyeClosed } from 'react-icons/lu';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthContext } from '../../../context/authContext.js';
import { LoginPage } from './LoginPage.jsx';
import { RequireAuth } from '../../guards/RequireAuth.jsx';
import { RequireRole } from '../../guards/RequireRole.jsx';

const login = vi.fn();

function LoginTestAuth({ children }) {
  const [user, setUser] = useState(null);
  async function authenticate(credentials) {
    const authenticatedUser = await login(credentials);
    setUser(authenticatedUser);
    return authenticatedUser;
  }
  return <AuthContext.Provider value={{ login: authenticate, user, isAuthenticated: Boolean(user), isInitializing: false }}>{children}</AuthContext.Provider>;
}

function renderLogin(from) {
  render(
    <LoginTestAuth>
      <MemoryRouter initialEntries={[{ pathname: '/inicia-sesion', state: from ? { from } : null }]}>
        <Routes>
          <Route path="/inicia-sesion" element={<LoginPage />} />
          <Route path="/panel" element={<h1>Panel de usuario</h1>} />
          <Route path="/perfil" element={<h1>Mi perfil</h1>} />
          <Route path="/encuestas/crear" element={<RequireAuth><RequireRole allowedRoles={['EDITOR']}><h1>Crear encuesta</h1></RequireRole></RequireAuth>} />
          <Route path="/superadmin/usuarios" element={<RequireAuth><RequireRole allowedRoles={['SUPERADMIN']}><h1>Gestión de usuarios</h1></RequireRole></RequireAuth>} />
          <Route path="/403" element={<h1>Acceso no permitido</h1>} />
        </Routes>
      </MemoryRouter>
    </LoginTestAuth>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => login.mockReset());

  it('muestra el ojo abierto para revelar la contraseña y el cerrado para ocultarla', async () => {
    const user = userEvent.setup();
    renderLogin();
    const password = screen.getByLabelText('Contraseña');
    const showButton = screen.getByRole('button', { name: 'Mostrar contraseña' });

    expect(password).toHaveAttribute('type', 'password');
    expect(showButton.querySelector('svg').outerHTML).toBe(renderToStaticMarkup(<LuEye aria-hidden="true" />));

    await user.click(showButton);

    const hideButton = screen.getByRole('button', { name: 'Ocultar contraseña' });
    expect(password).toHaveAttribute('type', 'text');
    expect(hideButton.querySelector('svg').outerHTML).toBe(renderToStaticMarkup(<LuEyeClosed aria-hidden="true" />));
  });

  it('valida los campos antes de intentar iniciar sesión', async () => {
    const user = userEvent.setup();
    renderLogin();
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(screen.getByText('Introduce tu correo electrónico.')).toBeVisible();
    expect(screen.getByText('Introduce tu contraseña.')).toBeVisible();
    expect(login).not.toHaveBeenCalled();
  });

  it('normaliza el correo y vuelve a la página solicitada', async () => {
    const user = userEvent.setup();
    login.mockResolvedValue({ id: 1 });
    renderLogin({ pathname: '/perfil' });
    await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), '  Ana@Example.com  ');
    await user.type(screen.getByLabelText('Contraseña'), 'pass123456');
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Mi perfil' })).toBeVisible();
    expect(login).toHaveBeenCalledWith({ email: 'ana@example.com', password: 'pass123456' });
  });

  it('muestra un fallo de autenticación y permite volver a intentarlo', async () => {
    const user = userEvent.setup();
    login.mockRejectedValueOnce(new Error('401'));
    renderLogin();
    await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), 'ana@example.com');
    await user.type(screen.getByLabelText('Contraseña'), 'pass123456');
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se ha podido iniciar sesión');
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeEnabled();
  });

  it.each(['ADMIN', 'SUPERADMIN'])('abre el panel al iniciar sesión como %s desde una ruta exclusiva de editores', async (role) => {
    const user = userEvent.setup();
    login.mockResolvedValue({ id: 1, role });
    renderLogin({ pathname: '/encuestas/crear' });
    await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), 'usuario@example.com');
    await user.type(screen.getByLabelText('Contraseña'), 'pass123456');
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Panel de usuario' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Acceso no permitido' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Crear encuesta' })).not.toBeInTheDocument();
  });

  it.each([
    ['EDITOR', '/encuestas/crear', 'Crear encuesta'],
    ['SUPERADMIN', '/superadmin/usuarios', 'Gestión de usuarios'],
    ['SUPERADMIN', undefined, 'Panel de usuario'],
  ])('mantiene el destino permitido para %s al volver a %s', async (role, pathname, heading) => {
    const user = userEvent.setup();
    login.mockResolvedValue({ id: 1, role });
    renderLogin(pathname ? { pathname } : undefined);
    await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), 'usuario@example.com');
    await user.type(screen.getByLabelText('Contraseña'), 'pass123456');
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { name: heading })).toBeVisible();
  });
});
