import { useEffect, useState } from 'react';
import { MdArrowForward, MdBarChart, MdCheckCircle, MdLockOutline, MdMoreVert, MdOutlineEdit, MdOutlineManageSearch, MdOutlinePerson, MdOutlineRemoveRedEye, MdPersonAdd } from 'react-icons/md';
import { Link } from 'react-router-dom';
import { resolveSurveyImageUrl } from '../../api/apiClient.js';
import { getCategories } from '../../api/categoriesApi.js';

import { CATEGORY_ICON_COMPONENTS, getCategoryIconName } from '../../utils/categoryIconOptions.js';
import { formatDate } from '../../utils/formatDate.js';
import { getSurveyAccessBadge } from '../../utils/surveyAccess.js';

import '../Table/Table.css';
import './SurveyCard.css';

/**
 * Representa una encuesta resumida mediante su imagen, estado, etiquetas y acciones principales.
 */
const LIFECYCLE_LABELS = {
  DRAFT: 'BORRADOR',
  OPEN: 'ABIERTA',
  CLOSED: 'CERRADA',
};

const VISIBILITY_LABELS = {
  LISTED: 'LISTADA',
  UNLISTED: 'OCULTA',
};

let pendingCategoriesRequest;

function loadSurveyCategories() {
  if (!pendingCategoriesRequest) {
    pendingCategoriesRequest = getCategories({ surveyOnly: true }).finally(() => {
      pendingCategoriesRequest = undefined;
    });
  }
  return pendingCategoriesRequest;
}

function useSurveyCategory(code) {
  const [category, setCategory] = useState(null);

  useEffect(() => {
    if (!code) return undefined;
    let active = true;
    loadSurveyCategories()
      .then((categories) => {
        if (active) setCategory(categories.find((item) => item.code === code) ?? null);
      })
      .catch(() => {});
    return () => { active = false; };
  }, [code]);

  return category?.code === code ? category : null;
}

function getCollaborationAction(survey, isRequesting) {
  if (survey.ownedByCurrentUser) return { disabled: true, label: 'Tu encuesta' };
  if (survey.collaborating) return { disabled: true, label: 'Ya colaboras' };
  if (survey.collaborationRequestPending) return { disabled: true, label: 'Solicitud pendiente' };
  return { disabled: isRequesting, label: isRequesting ? 'Enviando...' : 'Pedir colaborar' };
}

export function SurveyCard({
  survey, onEdit, onView, onSupervise, onRequestCollaboration, onMoreOptions,
  accessHref, headingLevel = 3, isRequesting = false, variant = 'editor',
}) {
  const normalizedHeadingLevel = Math.min(6, Math.max(2, Number(headingLevel) || 3));
  const Heading = `h${normalizedHeadingLevel}`;
  const { id, title = 'Sin título', lifecycleStatus, tags, imageKey, category: categoryCode } = survey;
  const category = useSurveyCategory(categoryCode);
  const CategoryIcon = categoryCode && CATEGORY_ICON_COMPONENTS[getCategoryIconName(category ?? { code: categoryCode })];
  const categoryLabel = category?.name ?? categoryCode?.replaceAll('_', ' ').toLocaleLowerCase('es').replace(/^./, (letter) => letter.toLocaleUpperCase('es'));
  const normalizedLifecycle = LIFECYCLE_LABELS[lifecycleStatus] ? lifecycleStatus : 'DRAFT';
  const accessBadge = getSurveyAccessBadge(normalizedLifecycle, survey.accessMode);
  const surveyTags = Array.isArray(tags) ? tags : [];
  const isAdminCard = variant === 'admin';
  const isDirectoryCard = variant === 'directory';
  const isPublicCard = variant === 'public';
  const isResultsCard = variant === 'results';
  const hasResultsDetails = Boolean(survey.editorName) || survey.responses !== undefined;
  const collaborationAction = getCollaborationAction(survey, isRequesting);
  const cardClassName = [
    'survey-card',
    isAdminCard && 'survey-card--admin',
    isDirectoryCard && 'survey-card--directory',
    isPublicCard && 'survey-card--public',
    isResultsCard && 'survey-card--results',
  ].filter(Boolean).join(' ');

  return (
    <article className={cardClassName}>
      <div className="survey-card-image-wrapper">
        {imageKey
          ? <img src={resolveSurveyImageUrl(imageKey)} alt="" className="survey-card-image" loading="lazy" decoding="async" />
          : <div className="survey-card-placeholder" aria-hidden="true" />}
      </div>

      <div className="survey-card-content">
        <Heading className="survey-card-title">{title}</Heading>

        {categoryCode && (
          <div className="survey-card-category">
            <CategoryIcon aria-hidden="true" />
            <span>{categoryLabel}</span>
          </div>
        )}

        {!isPublicCard && (
          <div className="survey-card-badges">
            <span className={`table-badge table-badge-${normalizedLifecycle.toLowerCase()}`}>
              {LIFECYCLE_LABELS[normalizedLifecycle]}
            </span>
            {isResultsCard && (
              <span className="survey-card-updated">
                Actualizada {formatDate(survey.updatedAt)}
              </span>
            )}
            {isAdminCard && <span className={`table-badge table-badge-${String(survey.visibility || 'UNLISTED').toLowerCase()}`}>
              {VISIBILITY_LABELS[survey.visibility] || 'OCULTA'}
            </span>}
          </div>
        )}

        {isPublicCard && (
          <p className="survey-card-description">
            {survey.description || 'Participa y comparte tu opinión en esta encuesta.'}
          </p>
        )}

        {(isAdminCard || isDirectoryCard || (isResultsCard && hasResultsDetails)) && <dl className="survey-card-admin-details">
          {(!isResultsCard || survey.editorName) && <div><dt>Editor</dt><dd>{survey.editorName || 'Sin propietario'}</dd></div>}
          {isAdminCard && <div><dt>Acceso</dt><dd><span className={`table-badge table-badge-${accessBadge.variant}`}>{accessBadge.label}</span></dd></div>}
          {(isAdminCard || isResultsCard) && survey.responses !== undefined && <div><dt>Respuestas</dt><dd>{survey.responses ?? 0}</dd></div>}
          {!isResultsCard && <div><dt>Actualizada</dt><dd>{formatDate(survey.updatedAt)}</dd></div>}
        </dl>}

        {surveyTags.length > 0 && <div className="survey-card-tags">
          {surveyTags.map((tag) => <span key={tag} className="survey-tag">{tag}</span>)}
        </div>}

        {isPublicCard && survey.accessMode !== 'PUBLIC' && (
          <p className="survey-card-access">
            {survey.accessMode === 'PASSWORD_PROTECTED'
              ? <MdLockOutline aria-hidden="true" />
              : <MdOutlinePerson aria-hidden="true" />}
            <span>
              {survey.accessMode === 'PASSWORD_PROTECTED'
                ? 'Acceso con contraseña'
                : 'Acceso con cuenta'}
            </span>
          </p>
        )}

        {isPublicCard && accessHref && (
          <Link
            className="survey-card-public-action"
            to={accessHref}
            aria-label={`Acceder a la encuesta: ${title}`}
          >
            <span>Acceder a la encuesta</span>
            <MdArrowForward aria-hidden="true" />
          </Link>
        )}

        {!isPublicCard && <div className="survey-card-actions">
          {isAdminCard && <>
            <button type="button" className="btn-survey-action btn-details" onClick={() => onView?.(id)}>
              <MdOutlineRemoveRedEye aria-hidden="true" /><span>Ver detalles</span>
            </button>
            <button type="button" className="btn-survey-action btn-supervise" onClick={() => onSupervise?.(id)}>
              <MdOutlineManageSearch aria-hidden="true" /><span>Supervisar</span>
            </button>
          </>}
          {isDirectoryCard && (
            <button type="button" className="btn-survey-action btn-collaborate" disabled={collaborationAction.disabled} onClick={() => onRequestCollaboration?.(id)}>
              {collaborationAction.disabled ? <MdCheckCircle aria-hidden="true" /> : <MdPersonAdd aria-hidden="true" />}
              <span>{collaborationAction.label}</span>
            </button>
          )}
          {isResultsCard && (
            <button type="button" className="btn-survey-action btn-results" onClick={() => onView?.(id)}>
              <MdBarChart aria-hidden="true" />
              <span>Ver resultados</span>
              <MdArrowForward aria-hidden="true" />
            </button>
          )}
          {!isAdminCard && !isDirectoryCard && !isResultsCard && <>
            <button type="button" className="btn-survey-action btn-edit" onClick={() => onEdit?.(id)}>
              <MdOutlineEdit aria-hidden="true" /><span>Editar</span>
            </button>
            {onView && (
              <button type="button" className="btn-survey-action btn-view" onClick={() => onView(id)}>
                <MdOutlineRemoveRedEye aria-hidden="true" /><span>Ver</span>
              </button>
            )}
          </>}

          {onMoreOptions && <button type="button" className="btn-survey-more" onClick={() => onMoreOptions(id)} aria-label={`Más opciones para ${title}`}>
            <MdMoreVert aria-hidden="true" />
          </button>}
        </div>}
      </div>
    </article>
  );
}
