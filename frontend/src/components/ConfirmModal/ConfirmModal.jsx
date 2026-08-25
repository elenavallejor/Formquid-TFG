import { useEffect } from 'react';

import './ConfirmModal.css';

/**
 * Componente de modal de confirmación.
 * 
 * Muestra un diálogo de confirmación para aprobar o rechazar una acción, 
 * bloqueando interacciones mientras la operación está en curso.
 */
export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  actionType,
  applicantName,
  isProcessing = false,
  errorMessage = '',
}) {
  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(event) {
      if (event.key === 'Escape' && !isProcessing) onClose();
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isProcessing, onClose]);

  if (!isOpen) return null;

  const isAcceptAction = actionType === 'ACCEPT';
  const title = isAcceptAction ? 'Aceptar solicitud' : 'Rechazar solicitud';
  const message = isAcceptAction
    ? `¿Confirmas que deseas aceptar la solicitud de ${applicantName || 'este usuario'}?`
    : `¿Confirmas que deseas rechazar la solicitud de ${applicantName || 'este usuario'}?`;
  const confirmLabel = isProcessing
    ? 'Procesando...'
    : isAcceptAction
      ? 'Aceptar'
      : 'Rechazar';

  function handleOverlayClick(event) {
    if (event.target === event.currentTarget && !isProcessing) onClose();
  }

  return (
    <div className="confirm-modal-overlay" role="presentation" onMouseDown={handleOverlayClick}>
      <section
        className="confirm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
        aria-describedby="confirm-modal-description"
        aria-busy={isProcessing}
      >
        <h2 id="confirm-modal-title" className="confirm-modal-title">{title}</h2>

        <p id="confirm-modal-description" className="confirm-modal-description">{message}</p>

        {errorMessage && (
          <p className="confirm-modal-error" role="alert" aria-live="assertive">{errorMessage}</p>
        )}

        <div className="confirm-modal-actions">
          <button type="button" className="confirm-modal-cancel" onClick={onClose} disabled={isProcessing}>
            Cancelar
          </button>

          <button
            type="button"
            className={`confirm-modal-confirm ${isAcceptAction ? 'confirm-modal-confirm-accept' : 'confirm-modal-confirm-deny'}`}
            onClick={onConfirm}
            disabled={isProcessing}
          >
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}