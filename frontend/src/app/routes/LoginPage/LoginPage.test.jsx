import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthContext } from '../../../context/authContext.js';
import { LoginPage } from './LoginPage.jsx';

const login = vi.fn();

function renderLogin(from) {
  render(
    <AuthContext.Provider value={{ login }}>
      <MemoryRouter initialEntries={[{ pathname: '/inicia-sesion', state: from ? { from } : null }]}>
        <Routes>
          <Route path="/inicia-sesion" element={<LoginPage />} />
          <Route path="/panel" element={<h1>Panel de usuario</h1>} />
          <Route path="/perfil" element={<h1>Mi perfil</h1>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => login.mockReset());

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
});
