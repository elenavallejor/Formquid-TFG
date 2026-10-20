import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { AuthContext } from '../../context/authContext.js';
import { RequireRole } from './RequireRole.jsx';

function renderProtectedRoute(auth) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/admin" element={<RequireRole allowedRoles={['ADMIN']}><h1>Administración</h1></RequireRole>} />
          <Route path="/403" element={<h1>Acceso no permitido</h1>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('RequireRole', () => {
  it('permite acceder al rol autorizado', () => {
    renderProtectedRoute({ user: { role: 'ADMIN' }, isInitializing: false });
    expect(screen.getByRole('heading', { name: 'Administración' })).toBeVisible();
  });

  it('redirige a la página de error cuando falta el permiso', () => {
    renderProtectedRoute({ user: { role: 'EDITOR' }, isInitializing: false });
    expect(screen.getByRole('heading', { name: 'Acceso no permitido' })).toBeVisible();
  });

  it('muestra carga mientras se restaura la sesión', () => {
    renderProtectedRoute({ user: null, isInitializing: true });
    expect(screen.getByRole('status')).toHaveTextContent('Cargando página…');
  });
});
