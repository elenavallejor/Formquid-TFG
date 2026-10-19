import { COUNTRIES, isKnownCountry } from '../../utils/countries.js';

import './CountrySelect.css';

/** Selector nativo compartido para el registro y la edición de cuenta. */
export function CountrySelect({
  id,
  name,
  value,
  onChange,
  disabled = false,
  icon: Icon,
  'aria-invalid': ariaInvalid,
  'aria-describedby': ariaDescribedBy,
}) {
  const selectedCountry = value ?? '';

  return (
    <div className={`country-select-wrapper${Icon ? ' country-select-wrapper--with-icon' : ''}`}>
      {Icon && <Icon className="country-select-icon" aria-hidden="true" />}
      <select
        id={id}
        name={name}
        className="country-select"
        value={selectedCountry}
        onChange={onChange}
        disabled={disabled}
        autoComplete="country"
        aria-invalid={ariaInvalid}
        aria-describedby={ariaDescribedBy}
      >
        <option value="">Selecciona un país</option>
        {selectedCountry && !isKnownCountry(selectedCountry) && (
          <option value={selectedCountry}>{selectedCountry}</option>
        )}
        {COUNTRIES.map(({ code, name: countryName }) => (
          <option key={code} value={code}>{countryName}</option>
        ))}
      </select>
    </div>
  );
}
