import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Restablece el scroll al inicio.
 */
export function useScrollToTop() {
  const { pathname } = useLocation();
  const previousPathRef = useRef(pathname);

  useEffect(() => {
    const hasChangedPage = previousPathRef.current !== pathname;
    previousPathRef.current = pathname;

    window.scrollTo(0, 0);

    if (hasChangedPage) {
      document.getElementById('main-content')?.focus({ preventScroll: true });
    }
  }, [pathname]);
}
