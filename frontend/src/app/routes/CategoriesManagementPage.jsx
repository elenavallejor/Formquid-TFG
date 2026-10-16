import { useEffect, useState } from 'react';
import {
  BsApp,
  BsBadgeAr,
  BsBookHalf,
  BsBookshelf,
  BsBoxSeam,
  BsCameraVideo,
  BsChatDots,
  BsChatText,
  BsController,
  BsCpu,
  BsEasel,
  BsHeadsetVr,
  BsJoystick,
  BsLaptop,
  BsMic,
  BsMortarboard,
  BsPcDisplay,
  BsPeople,
  BsPersonVcard,
  BsPhone,
  BsPlayCircle,
  BsProjector,
  BsRobot,
  BsSmartwatch,
  BsStars,
  BsTablet,
  BsTag,
  BsThreeDots,
  BsTv,
} from 'react-icons/bs';
import { TbCategoryPlus } from 'react-icons/tb';

import { createCategory, getCategories } from '../../api/categoriesApi.js';
import { Breadcrumbs } from '../../components/Breadcrumbs/Breadcrumbs.jsx';
import './CategoriesManagementPage.css';

const CATEGORY_ICONS = {
  ORDENADOR_SOBREMESA: BsPcDisplay,
  PORTATIL: BsLaptop,
  TABLETA: BsTablet,
  TELEFONO_INTELIGENTE: BsPhone,
  RELOJ_INTELIGENTE: BsSmartwatch,
  TELEVISION_INTELIGENTE: BsTv,
  CONSOLA_VIDEOJUEGOS: BsController,
  LECTOR_LIBROS_ELECTRONICOS: BsBookHalf,
  PIZARRA_INTERACTIVA: BsEasel,
  PROYECTOR_DIGITAL: BsProjector,
  PLATAFORMA_APRENDIZAJE: BsMortarboard,
  APLICACIONES_EDUCATIVAS: BsApp,
  LIBROS_TEXTO_DIGITALES: BsBookshelf,
  ROBOTICA_EDUCATIVA: BsRobot,
  IMPRESION_3D: BsBoxSeam,
  INTELIGENCIA_ARTIFICIAL: BsCpu,
  IA_GENERATIVA: BsStars,
  BOTS_CONVERSACION: BsChatDots,
  ASISTENTES_VOZ: BsMic,
  REALIDAD_VIRTUAL: BsHeadsetVr,
  REALIDAD_AUMENTADA: BsBadgeAr,
  REDES_SOCIALES: BsPeople,
  MENSAJERIA_INSTANTANEA: BsChatText,
  VIDEOCONFERENCIAS: BsCameraVideo,
  VIDEOJUEGOS: BsJoystick,
  PLATAFORMAS_TRANSMISION: BsPlayCircle,
  OTROS: BsThreeDots,
  DATOS_PERSONALES: BsPersonVcard,
};

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
            <label htmlFor="new-category-name">Nombre de la categoría</label>
            <div className="categories-form-controls">
              <input id="new-category-name" value={name} maxLength={100}
                onChange={(event) => setName(event.target.value)}
                placeholder="Ej.: tecnología asistiva" required />
              <button type="submit" disabled={saving || !name.trim()}>
                <TbCategoryPlus aria-hidden="true" />
                {saving ? 'Creando...' : 'Crear categoría'}
              </button>
            </div>
          </form>
          {error && <p role="alert" className="categories-error">{error}</p>}
          {success && <p role="status" className="categories-success">{success}</p>}
        </section>

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
              {categories.map((category) => {
                const Icon = CATEGORY_ICONS[category.code] ?? BsTag;
                return (
                  <li key={category.code}>
                    <span className="categories-list-icon"><Icon aria-hidden="true" /></span>
                    <span className="categories-list-name">{category.name}</span>
                    {category.personalData && <small>Solo catálogo</small>}
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </main>
  );
}
