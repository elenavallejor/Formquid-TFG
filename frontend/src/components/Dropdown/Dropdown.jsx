import { useId } from 'react';
import { MdKeyboardArrowDown } from 'react-icons/md';

import './Dropdown.css';

/**
 * Selector nativo con la apariencia común de FormQuid.
 *
 * Mantener la semántica de `select` evita recrear con ARIA la navegación,
 * el anuncio de la opción activa y la interacción propia de cada plataforma.
 */
export function Dropdown({
  children,
  className = '',
  disabled = false,
  id,
  name,
  onChange,
  value,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
  'aria-invalid': ariaInvalid,
}) {
  const generatedId = useId().replaceAll(':', '');
  const selectId = id || `dropdown-${generatedId}`;

  return (
    <div className={`custom-dropdown ${className}`.trim()}>
      <select
        id={selectId}
        className="custom-dropdown-trigger"
        name={name}
        value={value ?? ''}
        disabled={disabled}
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedBy}
        aria-invalid={ariaInvalid}
        onChange={onChange}
      >
        {children}
      </select>
      <MdKeyboardArrowDown className="custom-dropdown-icon" aria-hidden="true" />
    </div>
  );
}
