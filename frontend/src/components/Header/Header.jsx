import { Link } from 'react-router-dom'; 
import { ImUser } from 'react-icons/im';

import formQuidLogo from '../../assets/images/formquid-logo.png';
import './Header.css';

/**
 * Cabecera principal de FormQuid.
 */
export function Header() {
  return (
    <header id="main-header">
      <div className="header-content">
        <Link className="brand" to="/" aria-label="FormQuid - Inicio">
          <img
            className="brand-logo"
            src={formQuidLogo}
            alt=""
            aria-hidden="true"
          />
          <span className="brand-name" aria-hidden="true">
            FORM<span>QUID</span>
          </span>
        </Link>

        <nav className="header-navigation" aria-label="Navegación principal">
          
          <Link className="login-link" to="/inicia-sesion">
            <ImUser className="login-icon" aria-hidden="true" />
            <span>Inicia sesión</span>
          </Link>
          
          <Link className="request-account-link" to="/solicita-cuenta">
            Solicita tu cuenta
          </Link>
          
        </nav>
      </div>
    </header>
  );
}