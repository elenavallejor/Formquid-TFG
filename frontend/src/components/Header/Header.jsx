import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ImUser } from 'react-icons/im';
import {
  MdOutlineKeyboardArrowDown,
  MdOutlineLogout,
  MdOutlineNotificationsNone,
  MdOutlinePerson,
  MdOutlineDashboard,
} from 'react-icons/md';

import formQuidLogo from '../../assets/images/formquid-logo.png';

import { useAuth } from '../../context/AuthContext.jsx';
import { getUserInitials } from '../../utils/getUserInitials.js';
import { getUnreadNotificationsCount } from '../../api/notificationsApi.js';

import './Header.css';

const ROLE_LABELS = {
  EDITOR: 'Editor',
  ADMIN: 'Administrador',
  ADMINISTRADOR: 'Administrador',
  SUPERADMIN: 'Superadministrador',
  SUPER_ADMIN: 'Superadministrador',
  SUPERADMINISTRADOR: 'Superadministrador',
};

function getUserDisplayName(user) {
  return (
    user?.name ||
    user?.fullName ||
    user?.email ||
    'Usuario'
  );
}

/**
 * Cabecera principal de FormQuid.
 */
export function Header() {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  const {
    user,
    isAuthenticated,
    logout,
  } = useAuth();

  const userDisplayName = getUserDisplayName(user);
  const userRole = ROLE_LABELS[user?.role] ?? user?.role;

  useEffect(() => {
    let isMounted = true;

    if (isAuthenticated && user) {
      getUnreadNotificationsCount()
        .then((count) => {
          if (isMounted) setUnreadCount(count);
        })
        .catch((error) => {
          console.error('Error al obtener el número de notificaciones no leídas:', error);
        });
    }

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, user]);

  async function handleLogout() {
    try {
      await logout();

      navigate('/', {
        replace: true,
      });
    } catch (error) {
      console.error(
        'No se ha podido cerrar la sesión.',
        error
      );
    }
  }

  return (
    <header id="main-header">
      <div className="header-content">
        <Link
          className="brand"
          to="/"
          aria-label="FormQuid - Inicio"
        >
          <img
            className="brand-logo"
            src={formQuidLogo}
            alt=""
            aria-hidden="true"
          />

          <span
            className="brand-name"
            aria-hidden="true"
          >
            FORM<span>QUID</span>
          </span>
        </Link>

        <nav
          className="header-navigation"
          aria-label="Navegación principal"
        >
          {isAuthenticated && user ? (
            <>
              <button
                className="notification-button"
                type="button"
                aria-label="Ver notificaciones"
                onClick={() => navigate('/notificaciones')}
              >
                <span className="notification-label">NOTIFICACIONES</span>
                <MdOutlineNotificationsNone className="notification-icon" aria-hidden="true" />
                {unreadCount > 0 && (
                  <span className="notification-badge" title={`${unreadCount} no leídas`} />
                )}
              </button>

              <details className="user-menu">
                <summary className="user-menu-summary">
                  <span
                    className="user-avatar"
                    aria-hidden="true"
                  >
                    {getUserInitials(userDisplayName)}
                  </span>

                  <span className="user-header-information">
                    <span className="user-header-name">
                      {userDisplayName}
                    </span>

                    {userRole && (
                      <span className="user-header-role">
                        {userRole}
                      </span>
                    )}
                  </span>

                  <MdOutlineKeyboardArrowDown
                    className="user-menu-arrow"
                    aria-hidden="true"
                  />
                </summary>

                <div className="user-menu-dropdown">
                  <Link
                    className="user-menu-option"
                    to="/perfil"
                  >
                    <MdOutlinePerson
                      aria-hidden="true"
                    />

                    <span>Mi perfil</span>
                  </Link>

                  <Link
                    className="user-menu-option"
                    to="/panel"
                  >
                    <MdOutlineDashboard
                      aria-hidden="true"
                    />

                    <span>Mi panel</span>
                  </Link>

                  <button
                    className="user-menu-option logout-option"
                    type="button"
                    onClick={handleLogout}
                  >
                    <MdOutlineLogout
                      aria-hidden="true"
                    />

                    <span>Cerrar sesión</span>
                  </button>
                </div>
              </details>
            </>
          ) : (
            <>
              <Link
                className="login-link"
                to="/inicia-sesion"
              >
                <ImUser
                  className="login-icon"
                  aria-hidden="true"
                />

                <span>Inicia sesión</span>
              </Link>

              <Link
                className="request-account-link"
                to="/solicita-cuenta"
              >
                Solicita tu cuenta
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}