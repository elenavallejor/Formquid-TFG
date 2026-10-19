import { Navigate, useLocation } from 'react-router-dom';

import { useAuth } from '../../context/useAuth.js';

/**
 * Protege las rutas privadas y redirige al inicio de sesión
 * cuando no existe una sesión autenticada.
 */
export function RequireAuth({ children }) {
  const location = useLocation();
  const { isAuthenticated, isInitializing } = useAuth();

  if (!isInitializing && !isAuthenticated) {
    return (
      <Navigate
        to="/inicia-sesion"
        replace
        state={{ from: location }}
      />
    );
  }

  return children;
}