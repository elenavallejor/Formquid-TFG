import { Link } from 'react-router-dom';
import { MdArrowForward } from 'react-icons/md';

import './AdminPanelButton.css';

/**
 * Muestra una tarjeta de acceso del panel administrativo con información, contador opcional y enlace de navegación.
 */
const COUNTER_SINGULAR_MAP = {
  pendientes: 'pendiente',
  creadas: 'creada',
  registros: 'registro',
  solicitudes: 'solicitud',
};

const ELEMENT_SINGULAR_MAP = {
  solicitudes: 'solicitud',
  encuestas: 'encuesta',
  registros: 'registro',
};

export function AdminPanelButton({
  title,
  description,
  elementName,
  count,
  icon: Icon,
  to,
  theme = 'teal',
  fullWidth = false,
  hideCounter = false,
  buttonLabel,
  counterLabel = 'pendientes',
}) {
  const hasCount = Number.isFinite(count);
  const isSingular = hasCount && count === 1;

  const formattedCounterLabel = isSingular
    ? COUNTER_SINGULAR_MAP[counterLabel] || counterLabel.replace(/s$/, '')
    : counterLabel;

  const elementLabel = isSingular
    ? ELEMENT_SINGULAR_MAP[elementName] || elementName?.replace(/s$/, '')
    : elementName;

  const labelText =
    buttonLabel ||
    `Ver ${elementLabel || 'registros'}`;

  if (fullWidth) {
    return (
      <article className={`admin-panel-card full-width-variant theme-${theme}`}>
        <div className="admin-panel-card-left">
          <div className={`admin-panel-icon-container theme-${theme}`}>
            {Icon && (
              <Icon
                className="admin-panel-icon"
                aria-hidden="true"
              />
            )}
          </div>

          <div className="admin-panel-content">
            <h3 className="admin-panel-title">
              {title}
            </h3>

            <p className="admin-panel-description">
              {description}
            </p>
          </div>
        </div>

        <Link
          to={to}
          className={`admin-panel-action-button theme-${theme}`}
        >
          <span>{labelText}</span>
          <MdArrowForward
            className="admin-panel-action-icon"
            aria-hidden="true"
          />
        </Link>
      </article>
    );
  }

  return (
    <article className={`admin-panel-card theme-${theme}`}>
      <div className="admin-panel-card-top">
        <div className={`admin-panel-icon-container theme-${theme}`}>
          {Icon && (
            <Icon
              className="admin-panel-icon"
              aria-hidden="true"
            />
          )}
        </div>

        <div className="admin-panel-content">
          <h3 className="admin-panel-title">
            {title}
          </h3>

          <p className="admin-panel-description">
            {description}
          </p>
        </div>
      </div>

      <div className="admin-panel-card-bottom">
        {!hideCounter && (
          <div className="admin-panel-counter">
            <span
              className={`admin-panel-count-number theme-${theme}`}
              aria-label={
                hasCount
                  ? `${count} ${formattedCounterLabel}`
                  : 'Dato cargando'
              }
            >
              {hasCount ? count : '—'}
            </span>

            <span className="admin-panel-count-label">
              {formattedCounterLabel}
            </span>
          </div>
        )}

        <Link
          to={to}
          className={`admin-panel-action-button theme-${theme}`}
        >
          <span>{labelText}</span>
          <MdArrowForward
            className="admin-panel-action-icon"
            aria-hidden="true"
          />
        </Link>
      </div>
    </article>
  );
}