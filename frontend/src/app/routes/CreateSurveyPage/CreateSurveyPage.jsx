import { useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import '@fontsource/lexend/400.css';
import '@fontsource/opendyslexic/400.css';
import {
  MdAdd,
  MdClose,
  MdImage,
} from 'react-icons/md';

import { createSurvey, createSurveyFromImport, createSurveyFromTemplate } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { SurveyOriginModal } from '../../../components/SurveyOriginModal/SurveyOriginModal.jsx';
import { MAX_SURVEY_TAGS } from '../../surveyConstants.js';

import '../CreateBlockPage/CreateBlockPage.css';
import './CreateSurveyPage.css';

const INITIAL_FORM = {
  title: '',
  description: '',
  imageKey: '',
  fontFamily: 'TIMES_NEW_ROMAN',
  accessMode: 'PUBLIC',
  visibility: 'UNLISTED',
  accessPassword: '',
  tags: [],
  category: '',
};

const FONT_OPTIONS = [
  {
    value: 'TIMES_NEW_ROMAN',
    label: 'Times New Roman',
    fontFamily: '"Computer Modern", "Latin Modern Roman", Georgia, "Times New Roman", serif',
  },
  {
    value: 'OPEN_DYSLEXIC',
    label: 'OpenDyslexic',
    fontFamily: 'OpenDyslexic, sans-serif',
  },
  {
    value: 'LEXEND',
    label: 'Lexend',
    fontFamily: 'Lexend, sans-serif',
  },
  {
    value: 'ARIAL',
    label: 'Arial',
    fontFamily: 'Arial, sans-serif',
  },
];

function normalizeTag(value) {
  let normalized = value.trim().toLocaleLowerCase('es');
  if (normalized.startsWith('#')) normalized = normalized.slice(1);
  return normalized.normalize('NFKC').replace(/\s+/g, '-');
}

export function CreateSurveyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const importedSurvey = location.state?.importedSurvey ?? null;
  const selectedSurveyOrigin = location.state?.surveyOrigin ?? null;
  const initialOrigin = importedSurvey
    ? { type: 'IMPORT', imported: importedSurvey }
    : selectedSurveyOrigin;
  const saveLockRef = useRef(false);
  const originTriggerRef = useRef(null);
  const [formData, setFormData] = useState(() => ({
    ...INITIAL_FORM,
    title: importedSurvey?.title ?? '',
    description: importedSurvey?.description ?? '',
  }));
  const [origin, setOrigin] = useState(() => initialOrigin);
  const [isOriginOpen, setIsOriginOpen] = useState(() => !initialOrigin);
  const [tagInput, setTagInput] = useState('');
  const [tagError, setTagError] = useState('');
  const [imageHasError, setImageHasError] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  function updateForm(changes) {
    setFormData((current) => ({ ...current, ...changes }));
    setErrorMessage('');
  }

  function selectOrigin(nextOrigin) {
    setOrigin(nextOrigin);
    setIsOriginOpen(false);
  }

  function addTag() {
    setTagError('');
    const tag = normalizeTag(tagInput);
    if (!tag) return;
    if (!/^[\p{L}\p{N}-]+$/u.test(tag)) {
      setTagError('La etiqueta solo admite letras, números y guiones.');
      return;
    }
    if (tag.length > 50) {
      setTagError('La etiqueta no puede superar 50 caracteres.');
      return;
    }
    if (formData.tags.includes(tag)) {
      setTagError('Esta etiqueta ya está añadida.');
      return;
    }
    if (formData.tags.length >= MAX_SURVEY_TAGS) {
      setTagError(`Puedes añadir un máximo de ${MAX_SURVEY_TAGS} etiquetas.`);
      return;
    }
    updateForm({ tags: [...formData.tags, tag] });
    setTagInput('');
  }

  async function handleCreate() {
    setErrorMessage('');
    if (!origin) {
      setErrorMessage('Selecciona cómo quieres empezar la encuesta.');
      setIsOriginOpen(true);
      return;
    }
    if (!formData.title.trim()) {
      setErrorMessage('Introduce un título para la encuesta.');
      return;
    }
    if (formData.accessMode === 'PASSWORD_PROTECTED' && !formData.accessPassword.trim()) {
      setErrorMessage('Introduce la contraseña de acceso de la encuesta.');
      return;
    }
    if (saveLockRef.current) return;

    const payload = {
      title: formData.title.trim(),
      description: formData.description.trim() || null,
      imageKey: formData.imageKey.trim() || null,
      fontFamily: formData.fontFamily,
      accessMode: formData.accessMode,
      visibility: formData.visibility,
      accessPassword: formData.accessMode === 'PASSWORD_PROTECTED'
        ? formData.accessPassword
        : null,
      tags: formData.tags,
      ...(formData.category ? { category: formData.category } : {}),
    };

    saveLockRef.current = true;
    setIsSaving(true);
    try {
      let survey;
      if (origin.type === 'TEMPLATE') {
        survey = await createSurveyFromTemplate(origin.template.id, payload);
      } else if (origin.type === 'IMPORT') {
        survey = await createSurveyFromImport(payload, origin.imported.content);
      } else {
        survey = await createSurvey(payload);
      }
      navigate(`/encuestas/${survey.id}/editar`, {
        replace: true,
        state: { survey },
      });
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido crear la encuesta.');
    } finally {
      saveLockRef.current = false;
      setIsSaving(false);
    }
  }

  const originLabel = origin?.type === 'TEMPLATE'
    ? `Plantilla del catálogo: ${origin.template.title}`
    : origin?.type === 'IMPORT'
      ? `Documento ODT: ${origin.imported.fileName}`
      : 'Encuesta desde cero';

  return (
    <main className="create-survey-page">
      <div className="create-survey-container">
        <Breadcrumbs items={[
          { label: 'PANEL', to: '/panel' },
          { label: 'CREAR ENCUESTA' },
        ]} />

        <header className="create-survey-header">
          <div>
            <h1>CREA TU ENCUESTA</h1>
            <p>Configura sus datos básicos. La estructura y los colaboradores se gestionan después de crear el borrador.</p>
            <p className="create-survey-edit-note">Podrás volver a editar todos estos datos más adelante.</p>
          </div>
          <button type="button" className="btn-create-submit" disabled={isSaving || !origin} onClick={handleCreate}>
            <MdAdd aria-hidden="true" />
            <span>{isSaving ? 'Creando...' : origin?.type === 'IMPORT' ? 'Importar y crear borrador' : 'Crear encuesta'}</span>
          </button>
        </header>

        {errorMessage && (
          <div className="create-survey-error">
            <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />
          </div>
        )}

        <section className="create-survey-card" aria-labelledby="survey-origin-summary-title">
          <div className="create-survey-section-header">
            <div>
              <h2 id="survey-origin-summary-title">PUNTO DE PARTIDA</h2>
              <p>{origin ? originLabel : 'Todavía no has seleccionado cómo empezar.'}</p>
            </div>
            <button ref={originTriggerRef} type="button" className="create-survey-secondary-button" onClick={() => setIsOriginOpen(true)}>
              {origin ? 'Cambiar' : 'Seleccionar'}
            </button>
          </div>
        </section>

        <section className="create-survey-card" aria-labelledby="survey-general-title">
          <h2 id="survey-general-title">INFORMACIÓN GENERAL</h2>
          <div className="create-survey-field">
            <label htmlFor="survey-title">Título de la encuesta <span>*</span></label>
            <input
              id="survey-title"
              type="text"
              maxLength={200}
              value={formData.title}
              placeholder="Introduce un título para la encuesta..."
              onChange={(event) => updateForm({ title: event.target.value })}
            />
            <small>{formData.title.length}/200</small>
          </div>
          <div className="create-survey-field">
            <label htmlFor="survey-description">Descripción</label>
            <textarea
              id="survey-description"
              rows={5}
              value={formData.description}
              placeholder="Explica el propósito de la encuesta..."
              onChange={(event) => updateForm({ description: event.target.value })}
            />
          </div>
        </section>

        <section className="create-survey-card" aria-labelledby="survey-image-title">
          <h2 id="survey-image-title">IMAGEN</h2>
          <div className="create-survey-image-layout">
            <div className="create-survey-image-preview">
              {formData.imageKey && !imageHasError
                ? <img src={formData.imageKey} alt="Vista previa de la encuesta" onError={() => setImageHasError(true)} />
                : <MdImage aria-hidden="true" />}
            </div>
            <div className="create-survey-field">
              <label htmlFor="survey-image">URL de la imagen</label>
              <input
                id="survey-image"
                type="url"
                value={formData.imageKey}
                placeholder="https://ejemplo.com/imagen.jpg"
                onChange={(event) => {
                  setImageHasError(false);
                  updateForm({ imageKey: event.target.value });
                }}
              />
              <p>Se utilizará como portada de la encuesta.</p>
            </div>
          </div>
        </section>

        <section className="create-survey-card" aria-labelledby="survey-appearance-title">
          <h2 id="survey-appearance-title">APARIENCIA Y CLASIFICACIÓN</h2>
          <div className="create-survey-grid">
            <CategorySelect id="survey-category" surveyOnly defaultLabel="Heredar la plantilla u Otros" value={formData.category}
              onChange={(category) => updateForm({ category })} />
            <div className="create-survey-field">
              <label htmlFor="survey-font">Tipografía</label>
              <Dropdown id="survey-font" value={formData.fontFamily} onChange={(event) => updateForm({ fontFamily: event.target.value })}>
                {FONT_OPTIONS.map((font) => (
                  <option key={font.value} value={font.value} style={{ fontFamily: font.fontFamily }}>
                    {font.label}
                  </option>
                ))}
              </Dropdown>
            </div>
            <div className="create-survey-field create-survey-tags-field">
              <label htmlFor="survey-tag">Etiquetas ({formData.tags.length}/{MAX_SURVEY_TAGS})</label>
              <div className="create-survey-tag-input">
                <input
                  id="survey-tag"
                  type="text"
                  maxLength={50}
                  value={tagInput}
                  disabled={formData.tags.length >= MAX_SURVEY_TAGS}
                  placeholder="Ej.: inteligencia-artificial"
                  onChange={(event) => { setTagInput(event.target.value); setTagError(''); }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ',') {
                      event.preventDefault();
                      addTag();
                    }
                  }}
                />
                <button type="button" disabled={!tagInput.trim() || formData.tags.length >= MAX_SURVEY_TAGS} onClick={addTag}>Añadir</button>
              </div>
              {tagError && <p className="create-survey-inline-error" role="alert">{tagError}</p>}
              {formData.tags.length > 0 && (
                <ul className="create-survey-tags">
                  {formData.tags.map((tag) => (
                    <li key={tag}>
                      <span>#{tag}</span>
                      <button type="button" aria-label={`Eliminar etiqueta ${tag}`} onClick={() => updateForm({ tags: formData.tags.filter((item) => item !== tag) })}>
                        <MdClose aria-hidden="true" />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </section>

        <section className="create-survey-card" aria-labelledby="survey-access-title">
          <h2 id="survey-access-title">ACCESO Y VISIBILIDAD</h2>
          <fieldset className="create-survey-choice-group">
            <legend>Modo de acceso</legend>
            <label><input type="radio" name="access-mode" value="PUBLIC" checked={formData.accessMode === 'PUBLIC'} onChange={(event) => updateForm({ accessMode: event.target.value, accessPassword: '' })} /><span><strong>PÚBLICA</strong><small>Cualquier persona con acceso puede responder.</small></span></label>
            <label><input type="radio" name="access-mode" value="PASSWORD_PROTECTED" checked={formData.accessMode === 'PASSWORD_PROTECTED'} onChange={(event) => updateForm({ accessMode: event.target.value })} /><span><strong>PROTEGIDA CON CONTRASEÑA</strong><small>Se solicitará una contraseña antes de responder.</small></span></label>
            <label><input type="radio" name="access-mode" value="AUTHENTICATED" checked={formData.accessMode === 'AUTHENTICATED'} onChange={(event) => updateForm({ accessMode: event.target.value, accessPassword: '' })} /><span><strong>USUARIOS REGISTRADOS</strong><small>Solo podrán responder usuarios con una cuenta.</small></span></label>
          </fieldset>

          {formData.accessMode === 'PASSWORD_PROTECTED' && (
            <div className="create-survey-field create-survey-password">
              <label htmlFor="survey-password">Contraseña de acceso <span>*</span></label>
              <input id="survey-password" type="password" value={formData.accessPassword} autoComplete="new-password" onChange={(event) => updateForm({ accessPassword: event.target.value })} />
            </div>
          )}

          <fieldset className="create-survey-choice-group" disabled>
            <legend>Visibilidad</legend>
            <label><input type="radio" name="visibility" value="UNLISTED" checked readOnly /><span><strong>NO LISTADA</strong><small>Mientras sea un borrador no aparecerá en los listados públicos.</small></span></label>
          </fieldset>

          <p className="create-survey-security-note">
            Al publicar el borrador la encuesta pasará a estar listada. Después podrás cambiar su visibilidad desde la configuración.
          </p>
        </section>
      </div>

      <SurveyOriginModal
        isOpen={isOriginOpen}
        selectedOrigin={origin}
        onClose={() => {
          if (origin) {
            setIsOriginOpen(false);
          } else {
            navigate('/panel');
          }
        }}
        onImport={() => navigate('/encuestas/importar')}
        onSelect={selectOrigin}
        returnFocusRef={originTriggerRef}
      />
    </main>
  );
}
