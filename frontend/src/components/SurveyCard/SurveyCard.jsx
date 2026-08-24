import { MdOutlineEdit, MdOutlineRemoveRedEye, MdMoreVert } from 'react-icons/md';
import './SurveyCard.css';

const LIFECYCLE_LABELS = {
  DRAFT: 'Borrador',
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
};

export function SurveyCard({ survey, onEdit, onView, onMoreOptions }) {
  const { id, title, lifecycleStatus = 'DRAFT', tags = [], imageKey } = survey;

  const statusLabel = LIFECYCLE_LABELS[lifecycleStatus] || lifecycleStatus;
  const statusClass = lifecycleStatus.toLowerCase();

  return (
    <article className="survey-card">
      <div className="survey-card-image-wrapper">
        {imageKey ? (
          <img src={imageKey} alt="" className="survey-card-image" />
        ) : (
          <div className="survey-card-placeholder" aria-hidden="true" />
        )}
      </div>

      <div className="survey-card-content">
        <h3 className="survey-card-title">{title}</h3>

        <div className="survey-card-badges">
          <span className={`survey-status-badge status-${statusClass}`}>
            {statusLabel}
          </span>
        </div>

        {tags && tags.length > 0 && (
          <div className="survey-card-tags">
            {Array.from(tags).map((tag, idx) => (
              <span key={idx} className="survey-tag">
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

          <button
            type="button"
            className="btn-survey-more"
            onClick={() => onMoreOptions?.(id)}
            aria-label="Más opciones"
          >
            <MdMoreVert aria-hidden="true" />
          </button>
        </div>
      </div>
    </article>
  );
}