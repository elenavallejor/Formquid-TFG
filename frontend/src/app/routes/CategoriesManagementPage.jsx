import { useEffect, useState } from 'react';
import { TbCategoryPlus } from 'react-icons/tb';

import { createCategory, getCategories, updateCategoryIcon } from '../../api/categoriesApi.js';
import { Breadcrumbs } from '../../components/Breadcrumbs/Breadcrumbs.jsx';
import { Pagination } from '../../components/Pagination/Pagination.jsx';
import { CategoryIconPicker } from '../../components/CategoryIconPicker/CategoryIconPicker.jsx';
import {
  CATEGORY_ICON_COMPONENTS,
  DEFAULT_CATEGORY_ICON,
  INITIAL_CATEGORY_ICONS,
  getCategoryIconName,
} from '../../utils/categoryIconOptions.js';
import './CategoriesManagementPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 27];

function compareCategories(a, b) {
  if (a.code === 'OTROS') return b.code === 'OTROS' ? 0 : 1;
  if (b.code === 'OTROS') return -1;
  return a.name.localeCompare(b.name, 'es');
}

export function CategoriesManagementPage() {
  const [categories, setCategories] = useState([]);
  const [name, setName] = useState('');
  const [icon, setIcon] = useState(DEFAULT_CATEGORY_ICON);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [updatingCode, setUpdatingCode] = useState(null);
  const [updateFeedback, setUpdateFeedback] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const totalPages = Math.ceil(categories.length / itemsPerPage);
  const visibleCategories = categories.slice(
    currentPage * itemsPerPage,
    (currentPage + 1) * itemsPerPage,
  );

  useEffect(() => {
    let active = true;
    getCategories()
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? [...response].sort(compareCategories) : []);
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
      const category = await createCategory(trimmed, icon);
      const updatedCategories = [...categories, category].sort(compareCategories);
      setCategories(updatedCategories);
      setCurrentPage(Math.floor(updatedCategories.findIndex((item) => item.code === category.code) / itemsPerPage));
      setName('');
      setIcon(DEFAULT_CATEGORY_ICON);
      setSuccess(`Categoría «${category.name}» creada.`);
    } catch (cause) {
      setError(cause.message || 'No se ha podido crear la categoría.');
    } finally {
      setSaving(false);
    }
  }

  async function handleIconChange(category, nextIcon) {
    if (Object.hasOwn(INITIAL_CATEGORY_ICONS, category.code)
      || updatingCode || nextIcon === getCategoryIconName(category)) return;
    setUpdatingCode(category.code);
    setUpdateFeedback(null);
    try {
      const updated = await updateCategoryIcon(category.code, nextIcon);
      setCategories((current) => current.map((item) => (
        item.code === category.code ? updated : item
      )));
      setUpdateFeedback({ code: category.code, error: false, message: 'Icono actualizado.' });
    } catch (cause) {
      setUpdateFeedback({
        code: category.code,
        error: true,
        message: cause.message || 'No se ha podido actualizar el icono.',
      });
    } finally {
      setUpdatingCode(null);
    }
  }

  return (
    <main className="categories-page">
      <div className="categories-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'CATEGORÍAS' }]} />

        <header className="categories-page-header">
          <h1>CATEGORÍAS</h1>
          <p>Clasifica encuestas y elementos del catálogo por tecnología.</p>
        </header>

        <section className="categories-create-card" aria-labelledby="categories-create-title">
          <div className="categories-create-heading">
            <span className="categories-create-icon"><TbCategoryPlus aria-hidden="true" /></span>
            <div>
              <h2 id="categories-create-title">NUEVA CATEGORÍA</h2>
              <p>Añade una tecnología para clasificar encuestas y elementos del catálogo.</p>
            </div>
          </div>
          <form className="categories-form" onSubmit={handleSubmit}>
            <div className="categories-form-controls">
              <div className="categories-form-field categories-form-field--name">
                <label htmlFor="new-category-name">Nombre de la categoría</label>
                <input id="new-category-name" value={name} maxLength={100}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Ej.: gafas inteligentes" required />
              </div>
              <div className="categories-form-field categories-form-field--icon">
                <span className="categories-form-label">Icono</span>
                <CategoryIconPicker value={icon} onChange={setIcon} label="Icono de la categoría" />
              </div>
              <button className="categories-form-submit" type="submit" disabled={saving || !name.trim()}>
                <TbCategoryPlus aria-hidden="true" />
                {saving ? 'Creando...' : 'Crear categoría'}
              </button>
            </div>
          </form>
        </section>

        {error && <p role="alert" className="categories-error">{error}</p>}
        {success && <p role="status" className="categories-success">{success}</p>}

        <section className="categories-available" aria-labelledby="categories-available-title" aria-busy={loading}>
          <div className="categories-section-heading">
            <h2 id="categories-available-title">CATEGORÍAS DISPONIBLES</h2>
            {!loading && <span>{categories.length} {categories.length === 1 ? 'categoría' : 'categorías'}</span>}
          </div>
          {loading ? (
            <div className="categories-empty" role="status">Cargando categorías...</div>
          ) : categories.length === 0 ? (
            !error && <div className="categories-empty" role="status">Todavía no hay categorías disponibles.</div>
          ) : (
            <ul className="categories-list">
              {visibleCategories.map((category) => {
                const initialIconName = INITIAL_CATEGORY_ICONS[category.code];
                const InitialIcon = CATEGORY_ICON_COMPONENTS[initialIconName];
                return (
                  <li key={category.code} aria-busy={updatingCode === category.code}>
                    {InitialIcon ? (
                      <span className="categories-list-icon" aria-hidden="true"><InitialIcon /></span>
                    ) : (
                      <CategoryIconPicker
                        value={getCategoryIconName(category)}
                        onChange={(nextIcon) => handleIconChange(category, nextIcon)}
                        label={`Cambiar icono de ${category.name}`}
                        compact
                        disabled={Boolean(updatingCode)}
                      />
                    )}
                    <span className="categories-list-name">{category.name}</span>
                    {category.personalData && <small>Solo catálogo</small>}
                    {updateFeedback?.code === category.code && (
                      <span
                        className={`categories-update-feedback${updateFeedback.error ? ' categories-update-feedback--error' : ''}`}
                        role={updateFeedback.error ? 'alert' : 'status'}
                      >
                        {updateFeedback.message}
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
          {!loading && categories.length > 0 && (
            <Pagination
              ariaLabel="Paginación de categorías"
              currentPage={currentPage}
              itemLabel="categorías"
              onPageChange={setCurrentPage}
              onPageSizeChange={(size) => {
                setItemsPerPage(size);
                setCurrentPage(0);
              }}
              pageSize={itemsPerPage}
              pageSizeLabel="Categorías por página"
              pageSizeOptions={PAGE_SIZE_OPTIONS}
              totalItems={categories.length}
              totalPages={totalPages}
            />
          )}
        </section>
      </div>
    </main>
  );
}
