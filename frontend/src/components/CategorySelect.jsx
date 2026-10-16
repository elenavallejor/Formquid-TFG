import { useEffect, useState } from 'react';

import { getCategories } from '../api/categoriesApi.js';

export function CategorySelect({ id, value, onChange, surveyOnly = false, defaultLabel = 'Otros (por defecto)' }) {
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');

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
    <>
      <label htmlFor={id}>Categoría</label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{defaultLabel}</option>
        {categories.map((category) => (
          <option key={category.code} value={category.code}>{category.name}</option>
        ))}
      </select>
      {error && <small role="alert">{error}</small>}
    </>
  );
}
