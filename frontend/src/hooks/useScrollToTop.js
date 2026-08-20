import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Restablece el scroll al inicio.
 */
export function useScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
}