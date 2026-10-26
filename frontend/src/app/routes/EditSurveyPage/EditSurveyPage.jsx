import { useEffect, useRef, useState } from 'react';
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import '@fontsource/lexend/400.css';
import '../../../assets/styles/opendyslexic.css';
import { FaRegClipboard } from 'react-icons/fa';
import { MdAdd, MdCheck, MdClose, MdDeleteOutline, MdGroups, MdImage, MdOutlineSettings, MdOutlineVisibility, MdPublish, MdSave, MdSwapHoriz, MdVisibilityOff } from 'react-icons/md';

import {
  acceptCollaborationRequest,
  acceptPublicationRequest,
  createCollaborationRequest,
  getCollaborationRequests,
  getCollaborators,
  getPublicationRequests,
  rejectCollaborationRequest,
  rejectPublicationRequest,
  removeCollaborator,
  requestPublication,
  updateCollaboratorPermission,
} from '../../../api/collaborationsApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import { replaceSurveyContent } from '../../../api/surveyContentApi.js';
import { deleteSurvey, getSurveyById, updateAdminSurveyLifecycle, updateAdminSurveyVisibility, updateSurvey, updateSurveyLifecycle, uploadSurveyCover } from '../../../api/surveysApi.js';
import { transferSurveyOwnership } from '../../../api/usersApi.js';
import { resolveSurveyImageUrl } from '../../../api/apiClient.js';
import { MAX_SURVEY_TAGS } from '../../surveyConstants.js';
import { BackButton } from '../../../components/BackButton/BackButton.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { CategorySelect } from '../../../components/CategorySelect/CategorySelect.jsx';
import { CatalogSidebar } from '../../../components/CatalogSidebar/CatalogSidebar.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { PasswordVisibilityIcon } from '../../../components/PasswordVisibilityIcon/PasswordVisibilityIcon.jsx';
import { PasswordConfirmModal } from '../../../components/PasswordConfirmModal/PasswordConfirmModal.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyBuilder } from '../../../components/SurveyBuilder/SurveyBuilder.jsx';
import { SurveyPreviewModal } from '../../../components/SurveyPreviewModal/SurveyPreviewModal.jsx';
import { SurveyCoverInput } from '../../../components/SurveyCoverInput/SurveyCoverInput.jsx';
import { SurveyFontPreview } from '../../../components/SurveyFontPreview/SurveyFontPreview.jsx';
import { useAuth } from '../../../context/useAuth.js';
import { buildTemplateBlockFromCatalog, buildTemplateQuestionFromCatalog } from '../../../utils/templateBuilderUtils.js';
import { INITIAL_SURVEY_STRUCTURE, buildSurveyStructurePayload, buildSurveyStructureState, getSurveyPublicationValidationError, getSurveyStructureValidationError } from '../../../utils/surveyBuilderUtils.js';
import { formatDate, formatDateTime } from '../../../utils/formatDate.js';
import { getSurveyAccessBadge } from '../../../utils/surveyAccess.js';
import { normalizeTag } from '../../../utils/surveyTagUtils.js';
import { CATEGORY_ICON_COMPONENTS, getCategoryIconName } from '../../../utils/categoryIconOptions.js';

import '../CreateBlockPage/CreateBlockPage.css';
import '../CreateSurveyPage/CreateSurveyPage.css';
import '../ElementDetailPage/ElementDetailPage.css';
import '../../../components/Table/Table.css';
import './EditSurveyPage.css';

const TABS = [
  { id: 'SURVEY', label: 'Encuesta', icon: FaRegClipboard },
  { id: 'COLLABORATIONS', label: 'Colaboraciones', icon: MdGroups },
  { id: 'SETTINGS', label: 'Configuración', icon: MdOutlineSettings },
];

const FONT_OPTIONS = [
  { value: 'TIMES_NEW_ROMAN', label: 'Times New Roman', fontFamily: '"Computer Modern", "Latin Modern Roman", Georgia, "Times New Roman", serif' },
  { value: 'OPEN_DYSLEXIC', label: 'OpenDyslexic', fontFamily: 'OpenDyslexic, sans-serif' },
  { value: 'LEXEND', label: 'Lexend', fontFamily: 'Lexend, sans-serif' },
  { value: 'ARIAL', label: 'Arial', fontFamily: 'Arial, sans-serif' },
];

const LIFECYCLE_LABELS = { DRAFT: 'BORRADOR', OPEN: 'ABIERTA', CLOSED: 'CERRADA' };
const VISIBILITY_LABELS = { LISTED: 'LISTADA', UNLISTED: 'OCULTA' };

function SurveyDetailsView({ survey, structure, isLoading, errorMessage, imageHasError, onImageError }) {
  const lifecycle = survey?.lifecycleStatus || 'DRAFT';
  const accessBadge = getSurveyAccessBadge(lifecycle, survey?.accessMode);
  const tags = Array.isArray(survey?.tags) ? survey.tags : [];

  return (
    <main className="catalog-detail-page survey-detail-page">
      <div className="catalog-detail-container">
        <BackButton fallbackTo="/admin/encuestas" className="catalog-detail-back" />

        <Breadcrumbs
          className="catalog-detail-breadcrumb"
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'ENCUESTAS', to: '/admin/encuestas' },
            { label: 'VER DETALLES' },
          ]}
        />

        <header className="catalog-detail-page-header">
          <h1>VER ENCUESTA</h1>
          <p>Desde aquí puedes ver los detalles de la encuesta y su contenido</p>
        </header>

        {isLoading && <div className="catalog-detail-loading"><Skeleton variant="list" count={3} hasActions={false} /></div>}

        {!isLoading && errorMessage && (
          <section className="catalog-detail-error" role="alert"><p>{errorMessage}</p></section>
        )}

        {!isLoading && survey && (
          <article className="catalog-detail-card">
            <header className="catalog-detail-title-bar survey-detail-title-bar">
              <div className="survey-detail-title-heading">
                <span className="catalog-detail-title-icon" aria-hidden="true"><FaRegClipboard /></span>
                <h2>{survey.title || 'Sin título'}</h2>
              </div>
              <span className={`table-badge table-badge-${lifecycle.toLocaleLowerCase()}`}>
                {LIFECYCLE_LABELS[lifecycle] || lifecycle}
              </span>
            </header>

            <div className="catalog-detail-body">
              <div className="survey-detail-cover">
                {survey.imageKey && !imageHasError
                  ? <img src={resolveSurveyImageUrl(survey.imageKey)} alt={`Portada de ${survey.title || 'la encuesta'}`} onError={onImageError} />
                  : <MdImage aria-hidden="true" />}
              </div>

              <p className="catalog-detail-description">{survey.description || 'Sin descripción.'}</p>

              <dl className="catalog-detail-metadata survey-detail-metadata">
                <div><dt>ESTADO</dt><dd><span className={`table-badge table-badge-${lifecycle.toLocaleLowerCase()}`}>{LIFECYCLE_LABELS[lifecycle] || lifecycle}</span></dd></div>
                <div><dt>ACCESO</dt><dd><span className={`table-badge table-badge-${accessBadge.variant}`}>{accessBadge.label}</span></dd></div>
                <div><dt>VISIBILIDAD</dt><dd><span className={`table-badge table-badge-${String(survey.visibility || 'UNLISTED').toLocaleLowerCase()}`}>{VISIBILITY_LABELS[survey.visibility] || survey.visibility || '—'}</span></dd></div>
                <div><dt>CREADA</dt><dd>{formatDateTime(survey.createdAt)}</dd></div>
                <div><dt>ÚLTIMA ACTUALIZACIÓN</dt><dd>{formatDateTime(survey.updatedAt)}</dd></div>
              </dl>

              <div className="survey-detail-tags" aria-label="Etiquetas de la encuesta">
                <strong>Etiquetas:</strong>
                {tags.length > 0
                  ? tags.map((tag) => <span key={tag} className="survey-detail-tag">#{tag}</span>)
                  : <span className="survey-detail-tags-empty">Sin etiquetas</span>}
              </div>

              <section className="catalog-detail-structure survey-detail-structure" aria-labelledby="survey-detail-structure-title">
                <header className="catalog-detail-structure-header">
                  <h3 id="survey-detail-structure-title">ESTRUCTURA DE LA ENCUESTA</h3>
                </header>
                <div className="catalog-detail-structure-content">
                  <SurveyBuilder data={structure} readOnly />
                </div>
              </section>
            </div>
          </article>
        )}
      </div>
    </main>
  );
}

function settingsFromSurvey(survey) {
  return {
    title: survey?.title || '',
    description: survey?.description || '',
    imageKey: survey?.imageKey || '',
    fontFamily: survey?.fontFamily || 'TIMES_NEW_ROMAN',
    accessMode: survey?.accessMode || 'PUBLIC',
    visibility: survey?.visibility || 'UNLISTED',
    accessPassword: '',
    tags: survey?.tags || [],
    category: survey?.category || '',
  };
}

function SurveyGeneralInfo({ title, category, description }) {
  const [categoryDetails, setCategoryDetails] = useState(null);

  useEffect(() => {
    if (!category) return undefined;
    let active = true;
    getCategories({ surveyOnly: true })
      .then((categories) => {
        if (active) setCategoryDetails(Array.isArray(categories) ? categories.find((item) => item.code === category) ?? null : null);
      })
      .catch(() => {});
    return () => { active = false; };
  }, [category]);

  const categoryName = categoryDetails && categoryDetails.code === category
    ? categoryDetails.name
    : category === 'OTROS'
      ? 'Otras tecnologías'
      : category?.replaceAll('_', ' ').toLocaleLowerCase('es').replace(/^./, (letter) => letter.toLocaleUpperCase('es')) || 'Sin categoría';
  const CategoryIcon = category && CATEGORY_ICON_COMPONENTS[getCategoryIconName(
    categoryDetails?.code === category ? categoryDetails : { code: category },
  )];

  return (
    <section className="edit-survey-panel-card edit-survey-general-card" aria-labelledby="edit-survey-summary-title">
      <div className="edit-survey-card-heading">
        <div>
          <h2 id="edit-survey-summary-title">INFORMACIÓN GENERAL</h2>
          <p>Puedes editar esta información en la configuración de la encuesta.</p>
        </div>
      </div>
      <div className="create-survey-field">
        <label htmlFor="edit-survey-summary-name">Título de la encuesta</label>
        <input id="edit-survey-summary-name" type="text" value={title} readOnly aria-readonly="true" />
      </div>
      <div className="create-survey-field">
        <label htmlFor="edit-survey-summary-category">Categoría</label>
        <div className={`edit-survey-category-value ${CategoryIcon ? 'has-icon' : ''}`}>
          {CategoryIcon && <CategoryIcon aria-hidden="true" />}
          <input id="edit-survey-summary-category" type="text" value={categoryName} readOnly aria-readonly="true" />
        </div>
      </div>
      <div className="create-survey-field">
        <span id="edit-survey-summary-description-label" className="edit-survey-readonly-label">Descripción</span>
        <div className={`edit-survey-readonly-description ${description ? '' : 'is-empty'}`} role="textbox" aria-readonly="true" aria-labelledby="edit-survey-summary-description-label">
          {description || 'Esta encuesta no tiene descripción.'}
        </div>
      </div>
    </section>
  );
}

function SurveyTagsEditor({ tags, readOnly, onChange }) {
  const [tagInput, setTagInput] = useState('');
  const [tagError, setTagError] = useState('');

  function addTag() {
    const tag = normalizeTag(tagInput);
    setTagError('');
    if (!tag) return;
    if (!/^[\p{L}\p{N}-]+$/u.test(tag)) return setTagError('La etiqueta solo admite letras, números y guiones.');
    if (tag.length > 50) return setTagError('La etiqueta no puede superar 50 caracteres.');
    if (tags.includes(tag)) return setTagError('Esta etiqueta ya está añadida.');
    if (tags.length >= MAX_SURVEY_TAGS) return setTagError(`Puedes añadir un máximo de ${MAX_SURVEY_TAGS} etiquetas.`);
    onChange([...tags, tag]);
    setTagInput('');
  }

  return (
    <section className="edit-survey-panel-card edit-survey-tags-card" aria-labelledby="edit-survey-tags-title">
      <div className="edit-survey-card-heading">
        <div>
          <h2 id="edit-survey-tags-title">ETIQUETAS</h2>
          <p>Clasifica la encuesta con hasta {MAX_SURVEY_TAGS} etiquetas.</p>
        </div>
        <span className="edit-survey-tags-count">{tags.length}/{MAX_SURVEY_TAGS}</span>
      </div>
      {!readOnly && <div className="create-survey-field edit-survey-tags-field">
        <div className="create-survey-tag-input">
          <input id="edit-survey-tag" type="text" maxLength={50} value={tagInput} disabled={tags.length >= MAX_SURVEY_TAGS} placeholder="Ej.: uso-cotidiano, colegio..." aria-label="Nueva etiqueta" onChange={(event) => { setTagInput(event.target.value); setTagError(''); }} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ',') { event.preventDefault(); addTag(); } }} />
          <button type="button" disabled={!tagInput.trim() || tags.length >= MAX_SURVEY_TAGS} onClick={addTag}>Añadir</button>
        </div>
      </div>}
      {tagError && <p className="create-survey-inline-error" role="alert">{tagError}</p>}
      {tags.length > 0
        ? <ul className="create-survey-tags">{tags.map((tag) => <li key={tag}><span>#{tag}</span>{!readOnly && <button type="button" aria-label={`Eliminar etiqueta ${tag}`} onClick={() => onChange(tags.filter((item) => item !== tag))}><MdClose aria-hidden="true" /></button>}</li>)}</ul>
        : <p className="edit-survey-tags-empty">Esta encuesta todavía no tiene etiquetas.</p>}
    </section>
  );
}

function SurveySettings({ data, coverFile, imageHasError, lifecycleStatus, onImageError, onChange, onFileChange, onError, onCoverBusyChange, showPolicySettings }) {
  const [isAccessPasswordVisible, setIsAccessPasswordVisible] = useState(false);
  const selectedFont = FONT_OPTIONS.find((font) => font.value === data.fontFamily) || FONT_OPTIONS[0];
  const accessNotes = [
    lifecycleStatus === 'DRAFT' ? 'Los borradores permanecen no listados. Al publicar la encuesta pasará a estar listada y podrás cambiar después su visibilidad.' : null,
    data.visibility === 'UNLISTED' ? 'Una encuesta oculta no es privada. Utiliza contraseña o acceso autenticado si necesitas confidencialidad.' : null,
  ].filter(Boolean);

  return (
    <div className="edit-survey-settings-stack">
      <section className="create-survey-card" aria-labelledby="edit-survey-general-title">
        <h2 id="edit-survey-general-title">INFORMACIÓN GENERAL</h2>
        <div className="create-survey-field">
          <label htmlFor="edit-survey-title">Título de la encuesta <span>*</span></label>
          <input id="edit-survey-title" type="text" maxLength={200} value={data.title} onChange={(event) => onChange({ title: event.target.value })} />
          <small>{data.title.length}/200</small>
        </div>
        <CategorySelect id="edit-survey-category" surveyOnly value={data.category}
          onChange={(category) => onChange({ category })} />
        <div className="create-survey-field">
          <label htmlFor="edit-survey-description">Descripción</label>
          <textarea id="edit-survey-description" rows={5} maxLength={1000} value={data.description} onChange={(event) => onChange({ description: event.target.value })} />
          <small>{data.description.length}/1000</small>
        </div>
      </section>

      <section className="create-survey-card" aria-labelledby="edit-survey-image-title">
        <h2 id="edit-survey-image-title">IMAGEN</h2>
        <SurveyCoverInput id="edit-survey-image" imageKey={data.imageKey} file={coverFile} imageHasError={imageHasError} enableGallery
          onImageError={onImageError} onUrlChange={(imageKey) => onChange({ imageKey }, true)}
          onFileChange={onFileChange} onError={onError} onBusyChange={onCoverBusyChange} />
      </section>

      <section className="create-survey-card" aria-labelledby="edit-survey-appearance-title">
        <h2 id="edit-survey-appearance-title">APARIENCIA</h2>
        <div className="create-survey-grid edit-survey-appearance-layout">
          <div className="create-survey-field">
            <label htmlFor="edit-survey-font">Tipografía</label>
            <Dropdown id="edit-survey-font" value={data.fontFamily} onChange={(event) => onChange({ fontFamily: event.target.value })}>
              {FONT_OPTIONS.map((font) => <option key={font.value} value={font.value} style={{ fontFamily: font.fontFamily, fontSize: font.value === 'OPEN_DYSLEXIC' ? '16.9px' : undefined }}>{font.label}</option>)}
            </Dropdown>
          </div>
          <SurveyFontPreview fontFamily={selectedFont.fontFamily} isOpenDyslexic={data.fontFamily === 'OPEN_DYSLEXIC'} inputId="edit-survey-preview-answer" />
        </div>
      </section>

      {showPolicySettings && <section className="create-survey-card" aria-labelledby="edit-survey-access-title">
        <h2 id="edit-survey-access-title">ACCESO Y VISIBILIDAD</h2>
        {accessNotes.length > 0 && <div className="edit-survey-access-notes">
          {accessNotes.map((note) => <p key={note} className="create-survey-access-description">{note}</p>)}
        </div>}
        <fieldset className="create-survey-choice-group">
          <legend>Modo de acceso</legend>
          <label><input type="radio" name="edit-access-mode" value="PUBLIC" checked={data.accessMode === 'PUBLIC'} onChange={(event) => onChange({ accessMode: event.target.value, accessPassword: '' })} /><span><strong>PÚBLICO</strong><small>Cualquier persona con acceso puede responder.</small></span></label>
          <label><input type="radio" name="edit-access-mode" value="PASSWORD_PROTECTED" checked={data.accessMode === 'PASSWORD_PROTECTED'} onChange={(event) => onChange({ accessMode: event.target.value })} /><span><strong>PROTEGIDO CON CONTRASEÑA</strong><small>Se solicitará una contraseña antes de responder.</small></span></label>
          <label><input type="radio" name="edit-access-mode" value="AUTHENTICATED" checked={data.accessMode === 'AUTHENTICATED'} onChange={(event) => onChange({ accessMode: event.target.value, accessPassword: '' })} /><span><strong>RESTRINGIDO A USUARIOS AUTENTICADOS</strong><small>Solo podrán responder usuarios con una cuenta.</small></span></label>
        </fieldset>
        {data.accessMode === 'PASSWORD_PROTECTED' && <div className="create-survey-field create-survey-password">
          <label htmlFor="edit-survey-password">Nueva contraseña de acceso</label>
          <div className="password-visibility-field">
            <input id="edit-survey-password" type={isAccessPasswordVisible ? 'text' : 'password'} value={data.accessPassword} autoComplete="new-password" placeholder="Déjala vacía para conservar la actual" onChange={(event) => onChange({ accessPassword: event.target.value })} />
            <button type="button" className="password-visibility-toggle" aria-label={isAccessPasswordVisible ? 'Ocultar nueva contraseña de acceso' : 'Mostrar nueva contraseña de acceso'} aria-pressed={isAccessPasswordVisible} onClick={() => setIsAccessPasswordVisible((visible) => !visible)}>
              <PasswordVisibilityIcon visible={isAccessPasswordVisible} />
            </button>
          </div>
        </div>}
        <fieldset className="create-survey-choice-group" disabled={lifecycleStatus === 'DRAFT'}>
          <legend>Visibilidad</legend>
          <label><input type="radio" name="edit-visibility" value="LISTED" checked={data.visibility === 'LISTED'} onChange={(event) => onChange({ visibility: event.target.value })} /><span><strong>LISTADA</strong><small>Aparecerá en los listados cuando se publique.</small></span></label>
          <label><input type="radio" name="edit-visibility" value="UNLISTED" checked={data.visibility === 'UNLISTED'} onChange={(event) => onChange({ visibility: event.target.value })} /><span><strong>OCULTA</strong><small>No aparecerá en listados, pero podrá abrirse mediante su enlace directo.</small></span></label>
        </fieldset>
      </section>}
    </div>
  );
}

function CollaboratorItem({ collaborator, isOwner, isDisabled, onPermissionChange, onRemove, onTransfer }) {
  const name = collaborator.name || collaborator.email || 'Usuario';

  return (
    <li>
      <span className="edit-survey-avatar">{name.slice(0, 1).toUpperCase()}</span>
      <span className="edit-survey-person-data">
        <strong>{collaborator.name || 'Usuario'}</strong>
        <small>{collaborator.email}</small>
        {!isOwner && (
          <span className={`edit-survey-permission-badge permission-${(collaborator.permission || 'EDIT').toLocaleLowerCase()}`}>
            {collaborator.permission === 'VIEW' ? 'Solo lectura' : 'Puede editar'}
          </span>
        )}
        <small>Desde el {formatDate(collaborator.joinedAt)}</small>
      </span>
      {isOwner && (
        <span className="edit-survey-collaborator-actions">
          <button
            type="button"
            className="edit-survey-transfer-button"
            disabled={isDisabled}
            onClick={() => onTransfer(collaborator)}
          >
            <MdSwapHoriz aria-hidden="true" />Transferir autoridad a {name}
          </button>
          <Dropdown
            className="edit-survey-permission-select"
            value={collaborator.permission}
            disabled={isDisabled}
            aria-label={`Permiso de ${name}`}
            onChange={(event) => onPermissionChange(collaborator, event.target.value)}
          >
            <option value="VIEW">Solo lectura</option>
            <option value="EDIT">Puede editar</option>
          </Dropdown>
          <button
            type="button"
            className="edit-survey-icon-danger"
            aria-label={`Revocar la colaboración de ${name}`}
            disabled={isDisabled}
            onClick={() => onRemove(collaborator)}
          >
            <MdDeleteOutline aria-hidden="true" />
          </button>
        </span>
      )}
    </li>
  );
}

function CollaborationsPanel({ surveyId, isOwner, isLoading, collaborators, requests, publicationRequests, onReload, onSurveyReload, onError, onSuccess }) {
  const [email, setEmail] = useState('');
  const [permission, setPermission] = useState('EDIT');
  const [message, setMessage] = useState('');
  const [publicationMessage, setPublicationMessage] = useState('');
  const [pendingAction, setPendingAction] = useState('');
  const [transferTarget, setTransferTarget] = useState(null);
  const [transferError, setTransferError] = useState('');

  async function runAction(key, action, successMessage) {
    if (pendingAction) return false;
    setPendingAction(key);
    onError('');
    try {
      await action();
      await onReload();
      await onSurveyReload();
      onSuccess(successMessage);
      return true;
    } catch (error) {
      onError(error.message || 'No se ha podido completar la operación.');
      return false;
    } finally {
      setPendingAction('');
    }
  }

  async function invite(event) {
    event.preventDefault();
    const normalizedEmail = email.trim().toLocaleLowerCase('es');
    if (!normalizedEmail) return onError('Introduce el correo electrónico del usuario que quieres invitar.');
    const succeeded = await runAction('invite', () => createCollaborationRequest(surveyId, { email: normalizedEmail, permission, requestType: 'INVITATION', message: message.trim() || null }), 'Invitación interna enviada.');
    if (succeeded) { setEmail(''); setPermission('EDIT'); setMessage(''); }
  }

  function changePermission(collaborator, nextPermission) {
    if (nextPermission === collaborator.permission) return;
    void runAction(
      `permission-${collaborator.userId}`,
      () => updateCollaboratorPermission(surveyId, collaborator.userId, nextPermission),
      `Permiso de ${collaborator.name || collaborator.email || 'usuario'} actualizado.`,
    );
  }

  function revokeCollaboration(collaborator) {
    void runAction(
      `remove-${collaborator.userId}`,
      () => removeCollaborator(surveyId, collaborator.userId),
      'Colaboración revocada.',
    );
  }

  async function confirmTransfer(currentPassword) {
    if (!transferTarget || pendingAction) return;
    const collaborator = transferTarget;
    setPendingAction(`transfer-${collaborator.userId}`);
    setTransferError('');
    onError('');
    try {
      await transferSurveyOwnership(surveyId, collaborator.userId, currentPassword);
      await Promise.all([onReload(), onSurveyReload()]);
      onSuccess(`La autoridad se ha transferido a ${collaborator.name || collaborator.email || 'este usuario'}. Ahora colaboras con permiso de edición.`);
      setTransferTarget(null);
    } catch (error) {
      setTransferError(error.message || 'No se ha podido transferir la autoridad de la encuesta.');
    } finally {
      setPendingAction('');
    }
  }

  return (
    <div className="edit-survey-collaboration-grid">
      {isOwner ? (
        <section className="edit-survey-panel-card edit-survey-invite-card">
          <h2>INVITAR A UN COLABORADOR</h2><p>Elige si el usuario podrá modificar la encuesta o acceder únicamente en modo lectura.</p>
          <form onSubmit={invite}>
            <div className="create-survey-field"><label htmlFor="collaborator-email">Correo electrónico <span>*</span></label><input id="collaborator-email" type="email" maxLength={254} autoComplete="email" placeholder="usuario@ejemplo.com" value={email} onChange={(event) => setEmail(event.target.value)} /></div>
            <fieldset className="edit-survey-permission-options">
              <legend>Permiso del colaborador</legend>
              <label><input type="radio" name="collaboration-permission" value="EDIT" checked={permission === 'EDIT'} onChange={(event) => setPermission(event.target.value)} /><span><strong>Puede editar</strong><small>Podrá modificar la encuesta y solicitar su publicación.</small></span></label>
              <label><input type="radio" name="collaboration-permission" value="VIEW" checked={permission === 'VIEW'} onChange={(event) => setPermission(event.target.value)} /><span><strong>Solo lectura</strong><small>Podrá consultar la encuesta, pero no modificarla.</small></span></label>
            </fieldset>
            <div className="create-survey-field"><label htmlFor="collaboration-message">Mensaje opcional</label><textarea id="collaboration-message" rows={3} maxLength={1000} value={message} onChange={(event) => setMessage(event.target.value)} /></div>
            <button type="submit" className="edit-survey-primary-action" disabled={pendingAction === 'invite'}><MdAdd aria-hidden="true" />{pendingAction === 'invite' ? 'Enviando...' : 'Enviar invitación'}</button>
          </form>
        </section>
      ) : (
        <section className="edit-survey-panel-card edit-survey-invite-card">
          <h2>SOLICITAR PUBLICACIÓN</h2><p>La solicitud llegará al propietario, que podrá aceptarla o rechazarla.</p>
          <div className="create-survey-field"><label htmlFor="publication-message">Mensaje opcional</label><textarea id="publication-message" rows={3} maxLength={1000} value={publicationMessage} onChange={(event) => setPublicationMessage(event.target.value)} /></div>
          <button type="button" className="edit-survey-primary-action" disabled={pendingAction === 'publication'} onClick={() => runAction('publication', () => requestPublication(surveyId, publicationMessage), 'Solicitud de publicación enviada.')}><MdPublish aria-hidden="true" />{pendingAction === 'publication' ? 'Enviando...' : 'Solicitar publicación'}</button>
        </section>
      )}

      <section className="edit-survey-panel-card">
        <div className="edit-survey-card-heading"><div><h2>COLABORADORES</h2>{!isLoading && <p>{collaborators.length} en total</p>}</div></div>
        {isLoading ? <div className="edit-survey-list-skeleton"><Skeleton variant="list" count={2} hasActions={false} /></div> : collaborators.length === 0 ? <p className="edit-survey-empty-state">Esta encuesta todavía no tiene colaboradores.</p> : (
          <ul className="edit-survey-people-list">
            {collaborators.map((collaborator) => (
              <CollaboratorItem
                key={collaborator.userId}
                collaborator={collaborator}
                isOwner={isOwner}
                isDisabled={Boolean(pendingAction)}
                onPermissionChange={changePermission}
                onRemove={revokeCollaboration}
                onTransfer={(target) => { setTransferError(''); setTransferTarget(target); }}
              />
            ))}
          </ul>
        )}
      </section>

      <section className="edit-survey-panel-card edit-survey-requests-card">
        <div className="edit-survey-card-heading"><div><h2>SOLICITUDES PENDIENTES</h2>{!isLoading && <p>{requests.length} pendientes</p>}</div></div>
        {isLoading ? <div className="edit-survey-list-skeleton"><Skeleton variant="list" count={2} hasActions={false} /></div> : requests.length === 0 ? <p className="edit-survey-empty-state">No hay solicitudes de colaboración pendientes.</p> : <ul className="edit-survey-people-list">{requests.map((request) => {
          const canResolve = isOwner && request.requestType === 'ACCESS_REQUEST';
          return <li key={request.id}><span className="edit-survey-avatar"><MdGroups aria-hidden="true" /></span><span className="edit-survey-person-data"><strong>Usuario #{request.userId}</strong><small>{request.requestType === 'INVITATION' ? 'Invitación enviada' : 'Solicitud de acceso'}</small><span className={`edit-survey-permission-badge permission-${(request.permission || 'EDIT').toLocaleLowerCase()}`}>{request.permission === 'VIEW' ? 'Solo lectura' : 'Puede editar'}</span>{request.message && <small>“{request.message}”</small>}<small>{formatDate(request.createdAt)}</small></span>{canResolve && <span className="edit-survey-request-actions"><button type="button" className="edit-survey-icon-accept" aria-label="Aceptar solicitud" onClick={() => runAction(`accept-${request.id}`, () => acceptCollaborationRequest(surveyId, request.id), 'Solicitud aceptada.')}><MdCheck aria-hidden="true" /></button><button type="button" className="edit-survey-icon-danger" aria-label="Rechazar solicitud" onClick={() => runAction(`reject-${request.id}`, () => rejectCollaborationRequest(surveyId, request.id), 'Solicitud rechazada.')}><MdClose aria-hidden="true" /></button></span>}</li>;
        })}</ul>}
      </section>

      {isOwner && <section className="edit-survey-panel-card edit-survey-requests-card">
        <div className="edit-survey-card-heading"><div><h2>SOLICITUDES DE PUBLICACIÓN</h2>{!isLoading && <p>{publicationRequests.length} pendientes</p>}</div></div>
        {isLoading ? <div className="edit-survey-list-skeleton"><Skeleton variant="list" count={1} hasActions={false} /></div> : publicationRequests.length === 0 ? <p className="edit-survey-empty-state">No hay solicitudes de publicación pendientes.</p> : <ul className="edit-survey-people-list">{publicationRequests.map((request) => <li key={request.id}><span className="edit-survey-avatar"><MdPublish aria-hidden="true" /></span><span className="edit-survey-person-data"><strong>{request.requestedByName || `Usuario #${request.requestedByUserId}`}</strong><small>{request.requestedByEmail}</small>{request.message && <small>“{request.message}”</small>}<small>{formatDate(request.createdAt)}</small></span><span className="edit-survey-request-actions"><button type="button" className="edit-survey-icon-accept" aria-label="Aceptar publicación" onClick={() => runAction(`publish-${request.id}`, () => acceptPublicationRequest(surveyId, request.id), 'Publicación aprobada.')}><MdCheck aria-hidden="true" /></button><button type="button" className="edit-survey-icon-danger" aria-label="Rechazar publicación" onClick={() => runAction(`reject-publication-${request.id}`, () => rejectPublicationRequest(surveyId, request.id), 'Solicitud de publicación rechazada.')}><MdClose aria-hidden="true" /></button></span></li>)}</ul>}
      </section>}
      <PasswordConfirmModal
        isOpen={Boolean(transferTarget)}
        onClose={() => { setTransferTarget(null); setTransferError(''); }}
        onConfirm={confirmTransfer}
        isProcessing={pendingAction.startsWith('transfer-')}
        errorMessage={transferError}
        onClearError={() => setTransferError('')}
        title="Transferir autoridad"
        description={`¿Confirmas que deseas transferir la autoridad de esta encuesta a ${transferTarget?.name || transferTarget?.email || 'este usuario'}? Seguirás colaborando con permiso de edición. Introduce tu contraseña actual para continuar.`}
        confirmLabel="Transferir autoridad"
      />
    </div>
  );
}

export function EditSurveyPage() {
  const { surveyId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, isInitializing } = useAuth();
  const saveLockRef = useRef(false);
  const [activeTab, setActiveTab] = useState(({ configuracion: 'SETTINGS', colaboraciones: 'COLLABORATIONS' })[searchParams.get('seccion')] || 'SURVEY');
  const tabRefs = useRef(new Map());
  const [survey, setSurvey] = useState(null);
  const [structure, setStructure] = useState(INITIAL_SURVEY_STRUCTURE);
  const [settings, setSettings] = useState(settingsFromSurvey(null));
  const [collaborators, setCollaborators] = useState([]);
  const [requests, setRequests] = useState([]);
  const [publicationRequests, setPublicationRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingCollaborations, setIsLoadingCollaborations] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [hasStructureChanges, setHasStructureChanges] = useState(false);
  const [hasTagChanges, setHasTagChanges] = useState(false);
  const [hasSettingsChanges, setHasSettingsChanges] = useState(false);
  const [lifecycleConfirmation, setLifecycleConfirmation] = useState(null);
  const [visibilityConfirmation, setVisibilityConfirmation] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [isDeleted, setIsDeleted] = useState(false);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [imageHasError, setImageHasError] = useState(false);
  const [coverFile, setCoverFile] = useState(null);
  const [coverLoading, setCoverLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const isOwner = Boolean(user && survey && Number(user.id) === Number(survey.ownerId));
  const isAdministrator = user?.role === 'ADMIN' || user?.role === 'SUPERADMIN';
  const isViewingDetails = isAdministrator && searchParams.get('vista') === 'detalles';
  const isAdministrativeView = isAdministrator && !isOwner;
  const isSupervising = isAdministrativeView && !isViewingDetails;
  const canEditSurvey = !isAdministrativeView && (isOwner || Boolean(survey?.canEdit));
  const canPreviewSurvey = !isAdministrativeView && (isOwner || Boolean(survey?.canPreview));
  const hasUnsavedChanges = !isDeleted && (hasStructureChanges || hasTagChanges || hasSettingsChanges);
  const navigationBlocker = useBlocker(hasUnsavedChanges);

  useEffect(() => {
    if (isDeleted) navigate('/encuestas', { replace: true });
  }, [isDeleted, navigate]);

  async function loadCollaborations() {
    const [nextCollaborators, nextRequests, nextPublicationRequests] = await Promise.all([getCollaborators(surveyId), getCollaborationRequests(surveyId), getPublicationRequests(surveyId)]);
    setCollaborators(nextCollaborators || []);
    setRequests(nextRequests || []);
    setPublicationRequests(nextPublicationRequests || []);
  }

  async function reloadSurvey() {
    const response = await getSurveyById(surveyId);
    setSurvey(response);
  }

  useEffect(() => {
    let isMounted = true;
    async function load() {
      setIsLoading(true); setErrorMessage('');
      setHasStructureChanges(false); setHasTagChanges(false); setHasSettingsChanges(false);
      try {
        const response = await getSurveyById(surveyId);
        if (!isMounted) return;
        setSurvey(response); setStructure(buildSurveyStructureState(response)); setSettings(settingsFromSurvey(response));
      } catch (error) {
        if (isMounted) setErrorMessage(error.message || 'No se ha podido cargar la encuesta.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }
    if (!isInitializing) void load();
    return () => { isMounted = false; };
  }, [isInitializing, surveyId]);

  useEffect(() => {
    function handleBeforeUnload(event) {
      if (!hasUnsavedChanges) return;
      event.preventDefault();
      event.returnValue = '';
    }

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasUnsavedChanges]);

  useEffect(() => {
    if (!survey || activeTab !== 'COLLABORATIONS') return;
    let isMounted = true;
    Promise.all([getCollaborators(surveyId), getCollaborationRequests(surveyId), getPublicationRequests(surveyId)])
      .then(([nextCollaborators, nextRequests, nextPublicationRequests]) => {
        if (!isMounted) return;
        setCollaborators(nextCollaborators || []);
        setRequests(nextRequests || []);
        setPublicationRequests(nextPublicationRequests || []);
      })
      .catch((error) => {
        if (isMounted) setErrorMessage(error.message || 'No se han podido cargar las colaboraciones.');
      })
      .finally(() => {
        if (isMounted) setIsLoadingCollaborations(false);
      });
    return () => { isMounted = false; };
  }, [activeTab, survey, surveyId]);

  if (isViewingDetails) {
    return (
      <SurveyDetailsView
        survey={survey}
        structure={structure}
        isLoading={isLoading || isInitializing}
        errorMessage={errorMessage}
        imageHasError={imageHasError}
        onImageError={() => setImageHasError(true)}
      />
    );
  }

  const visibleTabs = isSupervising
    ? TABS.filter((tab) => tab.id !== 'COLLABORATIONS')
    : survey && !canEditSurvey ? TABS.filter((tab) => tab.id === 'SURVEY') : TABS;

  function clearMessages() { setErrorMessage(''); setSuccessMessage(''); }
  function addStructureItem(item, targetIndex) {
    clearMessages();
    setHasStructureChanges(true);
    setStructure((current) => {
      const items = [...current.items];
      const insertionIndex = Number.isInteger(targetIndex) ? Math.max(0, Math.min(targetIndex, items.length)) : items.length;
      items.splice(insertionIndex, 0, item);
      return { items };
    });
  }

  async function saveStructure() {
    const validationError = getSurveyStructureValidationError(structure);
    if (validationError) return setErrorMessage(validationError);
    if (saveLockRef.current) return;
    saveLockRef.current = true; setIsSaving(true); clearMessages();
    try {
      await replaceSurveyContent(surveyId, buildSurveyStructurePayload(structure));
      await updateSurvey(surveyId, {
        title: survey.title,
        description: survey.description || null,
        imageKey: survey.imageKey || null,
        fontFamily: survey.fontFamily,
        accessMode: survey.accessMode,
        visibility: survey.visibility,
        accessPassword: null,
        tags: settings.tags,
      });
      const refreshedSurvey = await getSurveyById(surveyId);
      setSurvey(refreshedSurvey); setStructure(buildSurveyStructureState(refreshedSurvey)); setSettings((current) => ({ ...current, tags: refreshedSurvey.tags || [] })); setHasStructureChanges(false); setHasTagChanges(false); setSuccessMessage('Cambios guardados correctamente.');
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido guardar la estructura.');
    } finally {
      saveLockRef.current = false; setIsSaving(false);
    }
  }

  async function saveSettings() {
    if (!settings.title.trim()) return setErrorMessage('Introduce un título para la encuesta.');
    if (saveLockRef.current || coverLoading) return;
    saveLockRef.current = true; setIsSaving(true); clearMessages();
    try {
      let response = await updateSurvey(surveyId, { title: settings.title.trim(), description: settings.description.trim() || null, imageKey: settings.imageKey.trim() || null, fontFamily: settings.fontFamily, accessMode: settings.accessMode, visibility: settings.visibility, accessPassword: settings.accessMode === 'PASSWORD_PROTECTED' ? settings.accessPassword || null : null, tags: settings.tags, category: settings.category });
      if (coverFile) response = await uploadSurveyCover(surveyId, coverFile);
      setCoverFile(null);
      setSurvey(response); setSettings(settingsFromSurvey(response)); setHasTagChanges(false); setHasSettingsChanges(false); setSuccessMessage('Configuración guardada correctamente.');
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido guardar la configuración.');
    } finally {
      saveLockRef.current = false; setIsSaving(false);
    }
  }

  async function changeLifecycle(lifecycleStatus) {
    if (saveLockRef.current || lifecycleStatus === survey.lifecycleStatus) return false;
    saveLockRef.current = true; setIsSaving(true); clearMessages();
    try {
      const response = isSupervising
        ? await updateAdminSurveyLifecycle(surveyId, lifecycleStatus)
        : await updateSurveyLifecycle(surveyId, lifecycleStatus);
      setSurvey(response);
      setSettings((current) => ({ ...current, visibility: response.visibility }));
      setSuccessMessage(`La encuesta ahora está ${LIFECYCLE_LABELS[lifecycleStatus].toLocaleLowerCase('es')}.`);
      return true;
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido cambiar el estado de la encuesta.');
      return false;
    } finally {
      saveLockRef.current = false; setIsSaving(false);
    }
  }

  async function confirmLifecycleChange() {
    if (!lifecycleConfirmation) return;
    const changed = await changeLifecycle(lifecycleConfirmation);
    if (changed) setLifecycleConfirmation(null);
  }

  function requestLifecycleChange(lifecycleStatus) {
    clearMessages();
    if (lifecycleStatus === 'OPEN') {
      if (hasUnsavedChanges) {
        setErrorMessage('Guarda todos los cambios antes de publicar la encuesta.');
        return;
      }
      const publicationError = getSurveyPublicationValidationError(structure);
      if (publicationError) {
        setErrorMessage(publicationError);
        setActiveTab('SURVEY');
        return;
      }
    }
    setLifecycleConfirmation(lifecycleStatus);
  }

  async function hideSurveyFromListings() {
    if (saveLockRef.current || !isSupervising) return;
    saveLockRef.current = true; setIsSaving(true); clearMessages();
    try {
      const response = await updateAdminSurveyVisibility(surveyId, 'UNLISTED');
      setSurvey(response); setSettings((current) => ({ ...current, visibility: response.visibility })); setVisibilityConfirmation(false); setSuccessMessage('La encuesta se ha ocultado de los listados públicos.');
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido ocultar la encuesta.');
    } finally {
      saveLockRef.current = false; setIsSaving(false);
    }
  }

  async function confirmDelete() {
    if (!isOwner || saveLockRef.current) return;
    saveLockRef.current = true;
    setIsDeleting(true);
    setDeleteError('');
    try {
      await deleteSurvey(surveyId);
      setIsDeleted(true);
    } catch (error) {
      setDeleteError(error.message || 'No se ha podido eliminar la encuesta.');
    } finally {
      saveLockRef.current = false;
      setIsDeleting(false);
    }
  }

  const primaryActionLabel = activeTab === 'SURVEY'
    ? canEditSurvey ? 'Guardar cambios' : ''
    : activeTab === 'SETTINGS' && canEditSurvey ? 'Actualizar configuración' : '';

  function handlePrimaryAction() {
    if (activeTab === 'SURVEY') void saveStructure();
    if (activeTab === 'SETTINGS') void saveSettings();
  }

  function selectTab(tabId) {
    setActiveTab(tabId);
    if (tabId === 'COLLABORATIONS' && survey) setIsLoadingCollaborations(true);
    clearMessages();
  }

  function handleTabKeyDown(event, currentTabId) {
    const currentIndex = visibleTabs.findIndex((tab) => tab.id === currentTabId);
    let nextIndex;

    if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % visibleTabs.length;
    if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + visibleTabs.length) % visibleTabs.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = visibleTabs.length - 1;
    if (nextIndex === undefined) return;

    event.preventDefault();
    const nextTabId = visibleTabs[nextIndex].id;
    selectTab(nextTabId);
    tabRefs.current.get(nextTabId)?.focus();
  }

  const addQuestion = (question, index) => addStructureItem(buildTemplateQuestionFromCatalog(question), index);
  const addBlock = (block, index) => addStructureItem(buildTemplateBlockFromCatalog(block), index);
  const isPageLoading = isLoading || isInitializing;
  const breadcrumbItems = isSupervising
    ? [
        { label: 'PANEL', to: '/panel' },
        { label: 'ENCUESTAS', to: '/admin/encuestas' },
        { label: 'SUPERVISAR' },
      ]
    : [{ label: 'PANEL', to: '/panel' }, { label: 'EDITAR ENCUESTA' }];
  const defaultTitle = isSupervising ? 'SUPERVISAR ENCUESTA' : 'EDITAR ENCUESTA';

  return (
    <div className={`create-block-layout edit-survey-layout ${activeTab !== 'SURVEY' ? 'without-catalog' : ''}`}>
      {activeTab === 'SURVEY' && canEditSurvey && <CatalogSidebar showQuestions showBlocks defaultOpen={false} onAddQuestion={addQuestion} onAddBlock={addBlock} />}
      <main className="create-block-main edit-survey-main">
        <div className="create-block-container edit-survey-container">
          <Breadcrumbs items={breadcrumbItems} />
          <header className="edit-survey-header">
            <div>
              <h1>{survey?.title || defaultTitle}</h1>
              <p>{survey ? (isSupervising ? 'Supervisa el estado y la visibilidad de esta encuesta.' : isOwner ? 'Eres propietario de esta encuesta.' : canEditSurvey ? 'Puedes editar esta encuesta como colaborador.' : 'Puedes consultar esta encuesta en modo de solo lectura.') : 'Gestiona su estructura, colaboradores y configuración.'}</p>
            </div>
            {(survey || primaryActionLabel) && <div className="edit-survey-header-actions">
              {(canPreviewSurvey || primaryActionLabel) && <div className="edit-survey-header-buttons">
                {canPreviewSurvey && <button type="button" className="edit-survey-preview-button" disabled={isPageLoading || !survey} onClick={() => setIsPreviewOpen(true)}><MdOutlineVisibility aria-hidden="true" />Previsualizar</button>}
                {primaryActionLabel && <button type="button" className="btn-create-submit" disabled={isSaving || isPageLoading || !survey || coverLoading} onClick={handlePrimaryAction}><MdSave aria-hidden="true" /><span>{isSaving ? 'Guardando...' : primaryActionLabel}</span></button>}
              </div>}
              {survey && <span className={`table-badge table-badge-${survey.lifecycleStatus.toLocaleLowerCase()}`}>{LIFECYCLE_LABELS[survey.lifecycleStatus]}</span>}
            </div>}
          </header>

          <nav className="edit-survey-tabs" role="tablist" aria-label="Secciones de edición">
            {visibleTabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  ref={(node) => {
                    if (node) tabRefs.current.set(tab.id, node);
                    else tabRefs.current.delete(tab.id);
                  }}
                  id={`survey-tab-${tab.id.toLocaleLowerCase()}`}
                  type="button"
                  role="tab"
                  aria-selected={activeTab === tab.id}
                  aria-controls={`survey-panel-${tab.id.toLocaleLowerCase()}`}
                  tabIndex={activeTab === tab.id ? 0 : -1}
                  className={activeTab === tab.id ? 'active' : ''}
                  onClick={() => selectTab(tab.id)}
                  onKeyDown={(event) => handleTabKeyDown(event, tab.id)}
                >
                  <Icon aria-hidden="true" />
                  {tab.label}
                </button>
              );
            })}
          </nav>

          {errorMessage && <div className="edit-survey-message"><ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} /></div>}
          {successMessage && <div className="edit-survey-success" role="status"><MdCheck aria-hidden="true" /><span>{successMessage}</span></div>}

          {activeTab === 'SURVEY' && <section id="survey-panel-survey" role="tabpanel" aria-labelledby="survey-tab-survey">
            {isPageLoading ? <div className="edit-survey-api-skeleton"><Skeleton variant="list" count={3} hasActions={false} /></div> : survey && <>{!canEditSurvey && <p className="edit-survey-structure-note">Tienes acceso de solo lectura. Puedes consultar la estructura, pero no modificarla.</p>}{canEditSurvey && survey.lifecycleStatus !== 'DRAFT' && <p className="edit-survey-structure-note">La estructura solo podrá guardarse si la encuesta todavía no tiene respuestas.</p>}<SurveyGeneralInfo title={survey.title} category={survey.category} description={survey.description || ''} /><SurveyTagsEditor tags={settings.tags} readOnly={!canEditSurvey} onChange={(tags) => { setSettings((current) => ({ ...current, tags })); setHasTagChanges(true); clearMessages(); }} /><SurveyBuilder data={structure} onChange={(nextStructure) => { setStructure(nextStructure); setHasStructureChanges(true); clearMessages(); }} onDropQuestionFromSidebar={addQuestion} onDropBlockFromSidebar={addBlock} readOnly={!canEditSurvey} /></>}
          </section>}
          {activeTab === 'COLLABORATIONS' && <section id="survey-panel-collaborations" role="tabpanel" aria-labelledby="survey-tab-collaborations">
            {isPageLoading ? <div className="edit-survey-api-skeleton"><Skeleton variant="list" count={3} hasActions={false} /></div> : survey && <CollaborationsPanel surveyId={surveyId} isOwner={isOwner} isLoading={isLoadingCollaborations} collaborators={collaborators} requests={requests} publicationRequests={publicationRequests} onReload={loadCollaborations} onSurveyReload={reloadSurvey} onError={setErrorMessage} onSuccess={setSuccessMessage} />}
          </section>}
          {activeTab === 'SETTINGS' && <section id="survey-panel-settings" role="tabpanel" aria-labelledby="survey-tab-settings">
            {isPageLoading ? <div className="edit-survey-api-skeleton"><Skeleton variant="list" count={4} hasActions={false} /></div> : survey && <>
              {!isSupervising && <SurveySettings data={settings} coverFile={coverFile} imageHasError={imageHasError} lifecycleStatus={survey.lifecycleStatus} onImageError={() => setImageHasError(true)} onChange={(changes, resetImageError = false) => { if (resetImageError) setImageHasError(false); setSettings((current) => ({ ...current, ...changes })); setHasSettingsChanges(true); clearMessages(); }} onFileChange={(file) => { setImageHasError(false); setCoverFile(file); setHasSettingsChanges(true); clearMessages(); }} onError={setErrorMessage} onCoverBusyChange={setCoverLoading} showPolicySettings={isOwner} />}
              {isOwner && <section className="edit-survey-panel-card edit-survey-lifecycle-card">
                <div className="edit-survey-lifecycle-copy">
                  <h2>CICLO DE VIDA</h2>
                  {survey.lifecycleStatus === 'OPEN' ? <>
                    <p>La encuesta está abierta y admite nuevas participaciones.</p>
                    <p className="edit-survey-publication-warning edit-survey-closing-warning">Una vez cerrada, la encuesta dejará de recibir respuestas. Podrás volver a publicarla más adelante.</p>
                  </> : <>
                    <p>Publica la encuesta para empezar a recibir participaciones.</p>
                    <p className="edit-survey-publication-warning">Una vez abierta, solo podrás editar completamente su estructura mientras no haya recibido ninguna respuesta.</p>
                  </>}
                </div>
                {survey.lifecycleStatus === 'OPEN'
                  ? <button type="button" className="edit-survey-publish-button edit-survey-close-button" disabled={isSaving} onClick={() => requestLifecycleChange('CLOSED')}><MdClose aria-hidden="true" />Cerrar encuesta</button>
                  : <button type="button" className="edit-survey-publish-button" disabled={isSaving} onClick={() => requestLifecycleChange('OPEN')}><MdPublish aria-hidden="true" />Publicar encuesta</button>}
              </section>}
              {isOwner && <section className="edit-survey-panel-card edit-survey-delete-card">
                <div>
                  <h2>ELIMINAR ENCUESTA</h2>
                  <p>Se eliminarán de forma permanente la encuesta y sus respuestas.</p>
                </div>
                <button type="button" className="edit-survey-publish-button edit-survey-close-button" disabled={isSaving || isDeleting} onClick={() => { clearMessages(); setDeleteError(''); setDeleteConfirmation(true); }}><MdDeleteOutline aria-hidden="true" />Eliminar encuesta</button>
              </section>}
              {isSupervising && <div className="edit-survey-supervision-stack">
                <section className="edit-survey-panel-card edit-survey-lifecycle-card">
                  <div className="edit-survey-lifecycle-copy">
                    <h2>CICLO DE VIDA</h2>
                    <p>Estado actual: <strong>{LIFECYCLE_LABELS[survey.lifecycleStatus]}</strong>.</p>
                    {survey.lifecycleStatus === 'DRAFT' && <p className="edit-survey-publication-warning">Los borradores solo pueden abrirse. Al abrirla se publicará y pasará a estar listada.</p>}
                    {survey.lifecycleStatus === 'OPEN' && <p className="edit-survey-publication-warning edit-survey-closing-warning">Como medida de supervisión puedes cerrarla. Dejará de recibir respuestas hasta que su propietario vuelva a publicarla.</p>}
                    {survey.lifecycleStatus === 'CLOSED' && <p className="edit-survey-publication-warning">La encuesta ya está cerrada y no admite nuevas respuestas. Su propietario podrá volver a publicarla.</p>}
                  </div>
                  {survey.lifecycleStatus === 'DRAFT' && <button type="button" className="edit-survey-publish-button" disabled={isSaving} onClick={() => requestLifecycleChange('OPEN')}><MdPublish aria-hidden="true" />Publicar encuesta</button>}
                  {survey.lifecycleStatus === 'OPEN' && <button type="button" className="edit-survey-publish-button edit-survey-close-button" disabled={isSaving} onClick={() => requestLifecycleChange('CLOSED')}><MdClose aria-hidden="true" />Cerrar encuesta</button>}
                </section>
                <section className="edit-survey-panel-card edit-survey-lifecycle-card">
                  <div className="edit-survey-lifecycle-copy">
                    <h2>VISIBILIDAD</h2>
                    <p>Visibilidad actual: <strong>{survey.visibility === 'UNLISTED' ? 'OCULTA' : 'LISTADA'}</strong>.</p>
                    {survey.visibility === 'UNLISTED'
                      ? <p className="edit-survey-publication-warning">{survey.lifecycleStatus === 'DRAFT' ? 'Los borradores no pueden aparecer en los listados públicos.' : 'La encuesta no aparece en los listados públicos. Su propietario podrá volver a marcarla como listada.'}</p>
                      : <p className="edit-survey-publication-warning edit-survey-closing-warning">Puedes ocultarla de los listados públicos por motivos de supervisión. Su enlace directo seguirá funcionando si el acceso y el estado lo permiten.</p>}
                  </div>
                  {survey.visibility !== 'UNLISTED' && <button type="button" className="edit-survey-publish-button edit-survey-close-button" disabled={isSaving} onClick={() => { clearMessages(); setVisibilityConfirmation(true); }}><MdVisibilityOff aria-hidden="true" />Ocultar encuesta</button>}
                </section>
              </div>}
            </>}
          </section>}
        </div>
      </main>
      <ConfirmModal
        isOpen={navigationBlocker.state === 'blocked'}
        onClose={() => navigationBlocker.reset()}
        onConfirm={() => navigationBlocker.proceed()}
        title="Salir sin guardar"
        message="Hay cambios sin guardar en la encuesta. ¿Confirmas que deseas salir de esta página?"
        confirmLabel="Salir sin guardar"
        variant="danger"
      />
      <SurveyPreviewModal
        isOpen={isPreviewOpen}
        survey={{ ...survey, ...settings }}
        structure={structure}
        onClose={() => setIsPreviewOpen(false)}
      />
      <ConfirmModal
        isOpen={Boolean(lifecycleConfirmation)}
        onClose={() => setLifecycleConfirmation(null)}
        onConfirm={confirmLifecycleChange}
        title={lifecycleConfirmation === 'CLOSED' ? 'Cerrar encuesta' : 'Publicar encuesta'}
        message={lifecycleConfirmation === 'CLOSED'
          ? '¿Confirmas que deseas cerrar la encuesta? Una vez cerrada, dejará de recibir respuestas. Podrás volver a publicarla más adelante.'
          : '¿Confirmas que deseas publicar la encuesta? Una vez abierta, solo podrás editar completamente su estructura mientras no haya recibido ninguna respuesta.'}
        confirmLabel={lifecycleConfirmation === 'CLOSED' ? 'Cerrar encuesta' : 'Publicar encuesta'}
        variant={lifecycleConfirmation === 'CLOSED' ? 'danger' : 'primary'}
        isProcessing={isSaving}
        errorMessage={errorMessage}
      />
      <ConfirmModal
        isOpen={visibilityConfirmation}
        onClose={() => setVisibilityConfirmation(false)}
        onConfirm={hideSurveyFromListings}
        title="Ocultar encuesta"
        message="¿Confirmas que deseas ocultar la encuesta? Dejará de aparecer en los listados públicos, aunque su enlace directo seguirá funcionando si el modo de acceso y el estado lo permiten."
        confirmLabel="Ocultar encuesta"
        variant="danger"
        isProcessing={isSaving}
        errorMessage={errorMessage}
      />
      <ConfirmModal
        isOpen={deleteConfirmation}
        onClose={() => { setDeleteConfirmation(false); setDeleteError(''); }}
        onConfirm={confirmDelete}
        title="Eliminar encuesta"
        message={`¿Confirmas que deseas eliminar “${survey?.title || 'esta encuesta'}”? Se perderán su contenido y sus respuestas. Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar encuesta"
        variant="danger"
        isProcessing={isDeleting}
        errorMessage={deleteError}
      />
    </div>
  );
}
