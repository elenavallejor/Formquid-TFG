import { Navigate, useLocation } from 'react-router-dom';

import { Skeleton } from '../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../context/useAuth.js';

/**
 * Restringe el acceso a una ruta según los roles permitidos para el usuario autenticado.
 */
export function RequireRole({ allowedRoles, children }) {
  const { user, isInitializing } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return (
      <main className="route-loading-state" aria-busy="true">
        <h1 className="sr-only">Cargando página</h1>
        <Skeleton variant="list" count={4} hasActions={false} label="Cargando página…" />
      </main>
    );
  }

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to={location.state?.returningFromLogin ? '/panel' : '/403'} replace />;
  }

  return children;
}
