import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ImUser } from 'react-icons/im';
import { MdOutlineDashboard, MdOutlineKeyboardArrowDown, MdOutlineLogout, MdOutlineNotificationsNone, MdOutlinePerson } from 'react-icons/md';

import formQuidLogo from '../../assets/images/formquid-logo.png';
import { useAuth } from '../../context/AuthContext.jsx';
import { getUnreadNotificationsCount } from '../../api/notificationsApi.js';
import { getUserInitials } from '../../utils/getUserInitials.js';

import './Header.css';

/**
 * Muestra la cabecera principal y adapta sus acciones al 
 * estado de autenticación del usuario.
 */
const ROLE_LABELS = {
  EDITOR: 'Editor',
  ADMIN: 'Administrador',
  SUPERADMIN: 'Superadministrador',
};

function getUserDisplayName(user) {
  return user?.fullName || user?.name || user?.email || 'Usuario';
}

export function Header() {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const userDisplayName = getUserDisplayName(user);
  const userRole = ROLE_LABELS[user?.role] ?? user?.role;

  useEffect(() => {
    let isMounted = true;

    if (!isAuthenticated || !user?.id) {
      setUnreadCount(0);
      return undefined;
    }

    getUnreadNotificationsCount()
      .then((count) => {
        if (isMounted) {
          setUnreadCount(count);
        }
      })
      .catch((error) => {
        console.error(
          'Error al obtener el número de notificaciones no leídas:',
          error,
        );
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, user?.id]);

  async function handleLogout() {
    if (isLoggingOut) return;

    try {
      setIsLoggingOut(true);
      await logout();
      navigate('/', { replace: true });
    } catch (error) {
      console.error('No se ha podido cerrar la sesión.', error);
    } finally {
      setIsLoggingOut(false);
    }
  }

  function handleMouseLeave(event) {
    event.currentTarget.removeAttribute('open');
  }

  const notificationLabel = unreadCount > 0
    ? `Ver notificaciones. ${unreadCount} sin leer.`
    : 'Ver notificaciones';

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

          <span className="brand-name" aria-hidden="true">
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
                aria-label={notificationLabel}
                onClick={() => navigate('/notificaciones')}
              >
                <span className="notification-label">
                  NOTIFICACIONES
                </span>

                <MdOutlineNotificationsNone
                  className="notification-icon"
                  aria-hidden="true"
                />

                {unreadCount > 0 && (
                  <span
                    className="notification-badge"
                    aria-hidden="true"
                  />
                )}
              </button>

              <details
                className="user-menu"
                onMouseLeave={handleMouseLeave}
              >
                <summary
                  className="user-menu-summary"
                  aria-label={`Menú de usuario de ${userDisplayName}`}
                >
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
                    <MdOutlinePerson aria-hidden="true" />
                    <span>Mi perfil</span>
                  </Link>

                  <Link
                    className="user-menu-option"
                    to="/panel"
                  >
                    <MdOutlineDashboard aria-hidden="true" />
                    <span>Mi panel</span>
                  </Link>

                  <button
                    className="user-menu-option logout-option"
                    type="button"
                    onClick={handleLogout}
                    disabled={isLoggingOut}
                  >
                    <MdOutlineLogout aria-hidden="true" />
                    <span>
                      {isLoggingOut ? 'Cerrando sesión...' : 'Cerrar sesión'}
                    </span>
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