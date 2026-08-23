import { useEffect, useRef } from 'react';
import { MdOutlineWarning, MdClose } from 'react-icons/md';
import './ConfirmModal.css';

export function ConfirmModal({ isOpen, onClose, onConfirm, actionType, applicantName }) {
  const modalRef = useRef(null);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const isAccept = actionType === 'ACCEPT';

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div
        className="modal-container"
        ref={modalRef}
        onClick={(e) => e.stopPropagation()}
      >
        <button className="modal-close-btn" onClick={onClose} aria-label="Cerrar ventana modal">
          <MdClose />
        </button>

        <div className={`modal-icon-wrapper ${isAccept ? 'type-accept' : 'type-deny'}`}>
          <MdOutlineWarning />
        </div>

        <h3 id="modal-title" className="modal-title">
          {isAccept
            ? `¿APROBAR REGISTRO DE ${applicantName?.toUpperCase()}?`
            : `¿DENEGAR Y ELIMINAR REGISTRO DE ${applicantName?.toUpperCase()}?`}
        </h3>

        <p className="modal-description">
          {isAccept
            ? `Se creará la cuenta de editor para ${applicantName} y esta acción no se podrá deshacer.`
            : `Se eliminará permanentemente la solicitud de ${applicantName} del sistema sin posibilidad de recuperación.`}
        </p>

        <div className="modal-actions">
          <button type="button" className="btn-modal-cancel" onClick={onClose}>
            CANCELAR
          </button>
          <button
            type="button"
            className={`btn-modal-confirm ${isAccept ? 'btn-modal-accept' : 'btn-modal-deny'}`}
            onClick={onConfirm}
          >
            {isAccept ? 'SÍ, APROBAR' : 'SÍ, ELIMINAR'}
          </button>
        </div>
      </div>
    </div>
  );
}