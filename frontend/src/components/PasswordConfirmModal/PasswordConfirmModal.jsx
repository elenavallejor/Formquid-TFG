import { useEffect, useRef, useState } from 'react';
import { MdOutlineVisibility, MdOutlineVisibilityOff } from 'react-icons/md';
import { ErrorMessage } from '../ErrorMessage/ErrorMessage.jsx';
import './PasswordConfirmModal.css';

/**
 * Solicita la contraseña actual para confirmar una acción y muestra los errores recibidos.
 */
export function PasswordConfirmModal({ isOpen, onClose, onConfirm, isProcessing = false, errorMessage = '', onClearError }) {
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const modalRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;

    const previousFocus = document.activeElement;
    setPassword('');
    setShowPassword(false);
    inputRef.current?.focus();

    return () => {
      if (previousFocus?.isConnected) {
        previousFocus.focus({ preventScroll: true });
      }
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(event) {
      if (event.key === 'Escape' && !isProcessing) {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== 'Tab') return;

      const focusableElements = modalRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled)');
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      const activeElement = document.activeElement;
      const focusOutside = !modalRef.current.contains(activeElement);

      if (!focusableElements.length) {
        event.preventDefault();
        modalRef.current.focus();
        return;
      }

      if (event.shiftKey && (activeElement === firstElement || activeElement === modalRef.current || focusOutside)) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && (activeElement === lastElement || activeElement === modalRef.current || focusOutside)) {
        event.preventDefault();
        firstElement.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isProcessing, onClose]);

  if (!isOpen) return null;

  function handleSubmit(event) {
    event.preventDefault();
    if (!password || isProcessing) return;
    onConfirm(password);
  }

  function handleOverlayMouseDown(event) {
    if (event.target === event.currentTarget && !isProcessing) {
      onClose();
    }
  }

  function handleClearError() {
    onClearError();
    if (isProcessing) {
      modalRef.current.focus();
    } else {
      inputRef.current.focus();
    }
  }

  return (
    <div className="pwd-modal-overlay" role="presentation" onMouseDown={handleOverlayMouseDown}>
      <section
        ref={modalRef}
        tabIndex={-1}
        className="pwd-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="pwd-modal-title"
        aria-describedby="pwd-modal-description"
        aria-busy={isProcessing}
      >
        <h2 id="pwd-modal-title" className="pwd-modal-title">
          Confirmar contraseña
        </h2>

        <p id="pwd-modal-description" className="pwd-modal-description">
          Introduce tu contraseña actual para confirmar esta acción.
        </p>

        <ErrorMessage message={errorMessage} onClose={handleClearError} />

        <form onSubmit={handleSubmit} className="pwd-modal-form">
          <div className="pwd-modal-field">
            <label htmlFor="modalCurrentPassword">Contraseña actual</label>
            <div className="pwd-modal-input-wrapper">
              <input
                ref={inputRef}
                type={showPassword ? 'text' : 'password'}
                id="modalCurrentPassword"
                name="currentPassword"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                disabled={isProcessing}
                required
              />
              <button
                type="button"
                className="btn-pwd-modal-eye"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Ocultar contraseña actual' : 'Mostrar contraseña actual'}
                disabled={isProcessing}
              >
                {showPassword ? <MdOutlineVisibilityOff aria-hidden="true" /> : <MdOutlineVisibility aria-hidden="true" />}
              </button>
            </div>
          </div>

          <div className="pwd-modal-actions">
            <button type="button" className="btn-pwd-modal-cancel" onClick={onClose} disabled={isProcessing}>
              Cancelar
            </button>
            <button type="submit" className="btn-pwd-modal-confirm" disabled={isProcessing || !password}>
              {isProcessing ? 'Procesando...' : 'Confirmar'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}