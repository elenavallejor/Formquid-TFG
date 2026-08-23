import { Link } from 'react-router-dom';
import { MdArrowForward } from 'react-icons/md';
import './AdminPanelButton.css';

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
  counterLabel = 'pendientes', // Permite personalizar el texto ("pendientes", "creadas", etc.)
}) {
  const labelText = buttonLabel || `Ver ${elementName || 'registros'}`;

  // Diseño destacado a ancho completo (sección Superadministrador)
  if (fullWidth) {
    return (
      <div className={`admin-panel-card full-width-variant theme-${theme}`}>
        <div className="admin-panel-card-left">
          <div className={`admin-panel-icon-container theme-${theme}`}>
            {Icon && <Icon className="admin-panel-icon" aria-hidden="true" />}
          </div>

          <div className="admin-panel-content">
            <h3 className="admin-panel-title">{title}</h3>
            <p className="admin-panel-description">{description}</p>
          </div>
        </div>

        <Link to={to} className={`admin-panel-action-button theme-${theme}`}>
          <span>{labelText}</span>
          <MdArrowForward className="admin-panel-action-icon" aria-hidden="true" />
        </Link>
      </div>
    );
  }

  // Diseño estándar en formato tarjeta
  return (
    <div className={`admin-panel-card theme-${theme}`}>
      <div className="admin-panel-card-top">
        <div className={`admin-panel-icon-container theme-${theme}`}>
          {Icon && <Icon className="admin-panel-icon" aria-hidden="true" />}
        </div>

        <div className="admin-panel-content">
          <h3 className="admin-panel-title">{title}</h3>
          <p className="admin-panel-description">{description}</p>
        </div>
      </div>

      <div className="admin-panel-card-bottom">
        {!hideCounter ? (
          <div className="admin-panel-counter">
            <span className={`admin-panel-count-number theme-${theme}`}>{count}</span>
            <span className="admin-panel-count-label">{counterLabel}</span>
          </div>
        ) : (
          <div />
        )}

        <Link to={to} className={`admin-panel-action-button theme-${theme}`}>
          <span>{labelText}</span>
          <MdArrowForward className="admin-panel-action-icon" aria-hidden="true" />
        </Link>
      </div>
    </div>
  );
}