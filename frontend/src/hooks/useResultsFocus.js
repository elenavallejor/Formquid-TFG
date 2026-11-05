import { useEffect, useRef } from 'react';

/** Recupera el foco perdido al recargar un listado después de usar su paginación. */
export function useResultsFocus(isLoading) {
  const resultsRef = useRef(null);
  const pendingFocusRef = useRef(false);

  useEffect(() => {
    if (isLoading || !pendingFocusRef.current) return;
    pendingFocusRef.current = false;
    if (document.activeElement === document.body) resultsRef.current?.focus();
  }, [isLoading]);

  function requestResultsFocus() {
    pendingFocusRef.current = true;
  }

  return { resultsRef, requestResultsFocus };
}
