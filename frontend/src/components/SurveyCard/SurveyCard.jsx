import {
  MdMoreVert,
  MdOutlineEdit,
  MdOutlineRemoveRedEye,
} from 'react-icons/md';

import './SurveyCard.css';

/**
 * Representa una encuesta resumida mediante su imagen, estado, etiquetas y acciones principales.
 */
const LIFECYCLE_LABELS = {
  DRAFT: 'Borrador',
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
};

export function SurveyCard({
  survey,
  onEdit,
  onView,
  onMoreOptions,
}) {
  const {
    id,
    title = 'Sin título',
    lifecycleStatus,
    tags,
    imageKey,
  } = survey;

  const normalizedLifecycle = LIFECYCLE_LABELS[lifecycleStatus]
    ? lifecycleStatus
    : 'DRAFT';

  const surveyTags = Array.isArray(tags) ? tags : [];

  return (
    <article className="survey-card">
      <div className="survey-card-image-wrapper">
        {imageKey ? (
          <img
            src={imageKey}
            alt=""
            className="survey-card-image"
            loading="lazy"
            decoding="async"
          />
        ) : (
          <div
            className="survey-card-placeholder"
            aria-hidden="true"
          />
        )}
      </div>

      <div className="survey-card-content">
        <h3 className="survey-card-title">
          {title}
        </h3>

        <div className="survey-card-badges">
          <span
            className={`survey-status-badge status-${normalizedLifecycle.toLowerCase()}`}
          >
            {LIFECYCLE_LABELS[normalizedLifecycle]}
          </span>
        </div>

        {surveyTags.length > 0 && (
          <div className="survey-card-tags">
            {surveyTags.map((tag) => (
              <span key={tag} className="survey-tag">
                {tag}
              </span>
            ))}
          </div>
        )}

        <div className="survey-card-actions">
          <button
            type="button"
            className="btn-survey-action btn-edit"
            onClick={() => onEdit?.(id)}
          >
            <MdOutlineEdit aria-hidden="true" />
            <span>Editar</span>
          </button>

          <button
            type="button"
            className="btn-survey-action btn-view"
            onClick={() => onView?.(id)}
          >
            <MdOutlineRemoveRedEye aria-hidden="true" />
            <span>Ver</span>
          </button>

          {onMoreOptions && (
            <button
              type="button"
              className="btn-survey-more"
              onClick={() => onMoreOptions(id)}
              aria-label={`Más opciones para ${title}`}
            >
              <MdMoreVert aria-hidden="true" />
            </button>
          )}
        </div>
      </div>
    </article>
  );
}