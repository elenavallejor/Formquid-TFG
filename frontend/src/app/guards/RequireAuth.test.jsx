import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { AuthContext } from '../../context/authContext.js';
import { RequireAuth } from './RequireAuth.jsx';

function renderPrivateRoute(auth) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/perfil']}>
        <Routes>
          <Route path="/perfil" element={<RequireAuth><h1>Mi perfil</h1></RequireAuth>} />
          <Route path="/inicia-sesion" element={<h1>Iniciar sesión</h1>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('RequireAuth', () => {
  it('permite la ruta a una sesión autenticada', () => {
    renderPrivateRoute({ isAuthenticated: true, isInitializing: false });
    expect(screen.getByRole('heading', { name: 'Mi perfil' })).toBeVisible();
  });

  it('redirige al inicio de sesión cuando no hay sesión', () => {
    renderPrivateRoute({ isAuthenticated: false, isInitializing: false });
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  it('espera la restauración de sesión antes de redirigir', () => {
    renderPrivateRoute({ isAuthenticated: false, isInitializing: true });
    expect(screen.getByRole('heading', { name: 'Mi perfil' })).toBeVisible();
  });
});
