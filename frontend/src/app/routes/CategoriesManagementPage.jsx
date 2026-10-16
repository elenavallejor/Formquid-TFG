import { useEffect, useState } from 'react';

import { createCategory, getCategories } from '../../api/categoriesApi.js';
import { Breadcrumbs } from '../../components/Breadcrumbs/Breadcrumbs.jsx';
import './CategoriesManagementPage.css';

export function CategoriesManagementPage() {
  const [categories, setCategories] = useState([]);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let active = true;
    getCategories()
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? response : []);
      })
      .catch((cause) => {
        if (active) setError(cause.message || 'No se han podido cargar las categorías.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed || saving) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const category = await createCategory(trimmed);
      setCategories((current) => [...current, category].sort((a, b) =>
        a.name.localeCompare(b.name, 'es')));
      setName('');
      setSuccess(`Categoría «${category.name}» creada.`);
    } catch (cause) {
      setError(cause.message || 'No se ha podido crear la categoría.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="categories-page">
      <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'CATEGORÍAS' }]} />
      <h1>CATEGORÍAS</h1>
      <p>Clasifica encuestas y elementos del catálogo por tecnología.</p>
      <form className="categories-form" onSubmit={handleSubmit}>
        <label htmlFor="new-category-name">Nueva categoría</label>
        <div>
          <input id="new-category-name" value={name} maxLength={100}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ej.: tecnología asistiva" required />
          <button type="submit" disabled={saving || !name.trim()}>
            {saving ? 'Creando...' : 'Crear categoría'}
          </button>
        </div>
      </form>
      {error && <p role="alert" className="categories-error">{error}</p>}
      {success && <p role="status" className="categories-success">{success}</p>}
      <h2>Categorías disponibles</h2>
      {loading ? <p>Cargando categorías...</p> : (
        <ul className="categories-list">
          {categories.map((category) => (
            <li key={category.code}>
              <span>{category.name}</span>
              {category.personalData && <small>Solo catálogo</small>}
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
