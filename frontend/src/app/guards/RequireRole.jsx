import { Navigate } from 'react-router-dom';

import { useAuth } from '../../context/AuthContext.jsx';

/**
 * Restringe el acceso a una ruta según los roles permitidos para el usuario autenticado.
 */
export function RequireRole({ allowedRoles, children }) {
  const { user, isInitializing } = useAuth();

  if (isInitializing) {
    return <main className="route-loading-state" aria-busy="true" />;
  }

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/403" replace />;
  }

  return children;
}