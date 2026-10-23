import { useEffect } from 'react';
import { useBlocker } from 'react-router-dom';

import { ConfirmModal } from './ConfirmModal.jsx';

export function UnsavedElementGuard({ hasChanges, elementName }) {
  const navigationBlocker = useBlocker(hasChanges);

  useEffect(() => {
    if (!hasChanges) return undefined;

    function handleBeforeUnload(event) {
      event.preventDefault();
      event.returnValue = '';
    }

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasChanges]);

  return (
    <ConfirmModal
      isOpen={navigationBlocker.state === 'blocked'}
      onClose={() => navigationBlocker.reset()}
      onConfirm={() => navigationBlocker.proceed()}
      title="Salir sin guardar"
      message={`Has realizado cambios. Si sales ahora, tu ${elementName} no se guardará. ¿Confirmas que deseas salir de esta página?`}
      confirmLabel="Salir sin guardar"
      variant="danger"
    />
  );
}
