import { Link } from 'react-router-dom';
import { MdChevronRight } from 'react-icons/md';

import './PanelButton.css';

/**
 * Muestra una tarjeta de navegación para acceder a una sección del panel.
 */
export function PanelButton({ title, description, icon: Icon, to }) {
  return (
    <Link to={to} className="panel-button">
      <div className="panel-button-icon-container" aria-hidden="true">
        {Icon && <Icon className="panel-button-icon" />}
      </div>

      <div className="panel-button-content">
        <h3 className="panel-button-title">{title}</h3>
        <p className="panel-button-description">{description}</p>
      </div>

      <MdChevronRight
        className="panel-button-arrow"
        aria-hidden="true"
      />
    </Link>
  );
}