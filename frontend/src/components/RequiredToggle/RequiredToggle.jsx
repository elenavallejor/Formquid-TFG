import './RequiredToggle.css';

export function RequiredToggle({ checked = false, onChange }) {
  return (
    <label className="required-toggle">
      <input
        type="checkbox"
        checked={Boolean(checked)}
        onChange={(event) => onChange?.(event.target.checked)}
      />
      <span>Obligatoria</span>
    </label>
  );
}
