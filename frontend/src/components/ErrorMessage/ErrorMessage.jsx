import { MdClose } from 'react-icons/md';
import './ErrorMessage.css';

/**
 * Muestra un mensaje de error que el usuario puede cerrar.
 */
export function ErrorMessage({ id, message, onClose }) {
  if (!message) return null;

  return (
    <div className="error-message">
      <p id={id} className="error-message-text" role="alert">{message}</p>
      <button type="button" className="error-message-close" aria-label="Cerrar mensaje de error" onClick={onClose}>
        <MdClose aria-hidden="true" />
      </button>
    </div>
  );
}
