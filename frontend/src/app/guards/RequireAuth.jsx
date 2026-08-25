import { Navigate, useLocation } from 'react-router-dom';

import { useAuth } from '../../context/AuthContext.jsx';

/**
 * Protege las rutas privadas y redirige al inicio de sesión 
 * cuando no existe una sesión autenticada.
 */
export function RequireAuth({ children }) {
  const location = useLocation();
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <main aria-busy="true" aria-live="polite">
        <p>Cargando...</p>
      </main>
    );
  }

  if (!isAuthenticated) {
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