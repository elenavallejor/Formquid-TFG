import { Link } from 'react-router-dom';
import { MdChevronRight } from 'react-icons/md';
import './PanelButton.css';

/**
 * Botón/Tarjeta interactiva para el panel principal.
 */
export function PanelButton({ title, description, icon: Icon, to }) {
  return (
    <Link to={to} className="panel-button">
      <div className="panel-button-icon-container">
        {Icon && <Icon className="panel-button-icon" aria-hidden="true" />}
      </div>

      <div className="panel-button-content">
        <h3 className="panel-button-title">{title}</h3>
        <p className="panel-button-description">{description}</p>
      </div>

      <MdChevronRight className="panel-button-arrow" aria-hidden="true" />
    </Link>
  );
}