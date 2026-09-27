import { useRef } from 'react';

import { useModalFocus } from '../../hooks/useModalFocus.js';

import './ConfirmModal.css';

/**
 * Componente de modal de confirmación.
 * Muestra el contenido de la acción indicado por la pantalla que lo utiliza.
 */
export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel,
  variant = 'primary',
  isProcessing = false,
  errorMessage = '',
}) {
  const dialogRef = useRef(null);
  const cancelRef = useRef(null);

  useModalFocus({
    isOpen,
    onClose,
    dialogRef,
    initialFocusRef: cancelRef,
    closeOnEscape: !isProcessing,
  });

  if (!isOpen) return null;

  function handleOverlayClick(event) {
    if (event.target === event.currentTarget && !isProcessing) onClose();
  }

  return (
    <div className="confirm-modal-overlay" role="presentation" onMouseDown={handleOverlayClick}>
      <section
        ref={dialogRef}
        tabIndex={-1}
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
          <button ref={cancelRef} type="button" className="confirm-modal-cancel" onClick={onClose} disabled={isProcessing}>
            Cancelar
          </button>

          <button
            type="button"
            className={`confirm-modal-confirm confirm-modal-confirm-${variant}`}
            onClick={onConfirm}
            disabled={isProcessing}
          >
            {isProcessing ? 'Procesando...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
