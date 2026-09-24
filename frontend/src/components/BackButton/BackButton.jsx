import { MdArrowBack } from 'react-icons/md';
import { useLocation, useNavigate } from 'react-router-dom';

import './BackButton.css';

/**
 * Vuelve a la pantalla anterior o navega a una ruta segura si no existe
 * historial dentro de la aplicación.
 */
export function BackButton({ fallbackTo = '/', label = 'Volver', className = '' }) {
  const location = useLocation();
  const navigate = useNavigate();

  function handleClick() {
    if (location.key === 'default') {
      navigate(fallbackTo, { replace: true });
      return;
    }

    navigate(-1);
  }

  return (
    <button
      type="button"
      className={`back-button ${className}`.trim()}
      onClick={handleClick}
      aria-label={label}
    >
      <MdArrowBack aria-hidden="true" />
      <span>{label}</span>
    </button>
  );
}
