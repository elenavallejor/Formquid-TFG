import { useEffect, useState } from 'react';

import { getCategories } from '../../api/categoriesApi.js';
import { CATEGORY_ICON_COMPONENTS, getCategoryIconName } from '../../utils/categoryIconOptions.js';
import { Dropdown } from '../Dropdown/Dropdown.jsx';

import './CategorySelect.css';

function CategoryLabel({ category }) {
  const Icon = CATEGORY_ICON_COMPONENTS[getCategoryIconName(category)];
  return <span className="category-select-option"><Icon aria-hidden="true" /><span>{category.name}</span></span>;
}

export function CategorySelect({ id, value, onChange, surveyOnly = false, defaultLabel = 'Otros (por defecto)', placeholder = '', showDefaultOption = true, required = false, validationError = '' }) {
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const errorMessage = validationError || error;

  useEffect(() => {
    let active = true;
    getCategories({ surveyOnly })
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? response : []);
      })
      .catch(() => {
        if (active) setError('No se han podido cargar las categorías.');
      });
    return () => { active = false; };
  }, [surveyOnly]);

  return (
    <div className="category-select">
      <label className="category-select-label" htmlFor={id}>Categoría{required && <> <span className="category-select-required" aria-hidden="true">*</span><span className="sr-only"> (obligatoria)</span></>}</label>
      <Dropdown id={id} value={value} onChange={(event) => onChange(event.target.value)}
        className={placeholder && !value ? 'category-select--placeholder' : ''}
        aria-invalid={validationError ? true : undefined} aria-describedby={errorMessage ? `${id}-error` : undefined}>
        {placeholder
          ? <option value="" disabled>{placeholder}</option>
          : showDefaultOption && <option value="" disabled={required}>{required ? defaultLabel : <CategoryLabel category={{ code: 'OTROS', name: defaultLabel }} />}</option>}
        {!showDefaultOption && value && !categories.some((category) => category.code === value) && (
          <option value={value}><CategoryLabel category={{ code: value, name: value === 'OTROS' ? 'Otros' : value }} /></option>
        )}
        {categories.map((category) => (
          <option key={category.code} value={category.code}><CategoryLabel category={category} /></option>
        ))}
      </Dropdown>
      {errorMessage && <small id={`${id}-error`} className="category-select-error" role="alert">{errorMessage}</small>}
    </div>
  );
}
