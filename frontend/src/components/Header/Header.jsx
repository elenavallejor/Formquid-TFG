import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ImUser } from 'react-icons/im';
import { MdOutlineDashboard, MdOutlineKeyboardArrowDown, MdOutlineLogout, MdOutlineNotificationsNone, MdOutlinePerson } from 'react-icons/md';

import { getUnreadNotificationsCount } from '../../api/notificationsApi.js';
import { NotificationsModal } from '../NotificationsModal/NotificationsModal.jsx';
import { useAuth } from '../../context/useAuth.js';
import { UserAvatar } from '../UserAvatar.jsx';

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

  const [notificationCount, setNotificationCount] = useState({ userId: null, count: 0 });
  const unreadCount = notificationCount.userId === user?.id ? notificationCount.count : 0;
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const userDisplayName = getUserDisplayName(user);
  const userRole = ROLE_LABELS[user?.role] ?? user?.role;

  useEffect(() => {
    let isMounted = true;

    if (!isAuthenticated || !user?.id) {
      return undefined;
    }

    function refreshCount() {
      getUnreadNotificationsCount()
      .then((count) => {
        if (isMounted) {
          setNotificationCount({ userId: user.id, count });
        }
      })
      .catch((error) => {
        console.error(
          'Error al obtener el número de notificaciones no leídas:',
          error,
        );
      });
    }

    refreshCount();
    window.addEventListener('notifications-changed', refreshCount);
    window.addEventListener('focus', refreshCount);

    return () => {
      isMounted = false;
      window.removeEventListener('notifications-changed', refreshCount);
      window.removeEventListener('focus', refreshCount);
    };
  }, [isAuthenticated, user?.id]);

  async function handleLogout() {
    if (isLoggingOut) return;

    try {
      setIsLoggingOut(true);
      navigate('/', { replace: true });
      await logout();
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
      <nav className="header-content" aria-label="Navegación principal">
        <Link
          className="brand"
          to="/"
          aria-label="FormQuid - Inicio"
        >
          <span className="brand-logo" aria-hidden="true" />

          <span className="brand-name" aria-hidden="true">
            FORM<span>QUID</span>
          </span>
        </Link>

        <div className="header-navigation">
          {isAuthenticated && user ? (
            <>
              <button
                className="notification-button"
                type="button"
                aria-label={notificationLabel}
                onClick={() => setNotificationsOpen(true)}
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
                  <UserAvatar className="user-avatar" name={userDisplayName} imageKey={user.profileImageKey} />

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
        </div>
      </nav>
      {isAuthenticated && notificationsOpen && <NotificationsModal onClose={() => setNotificationsOpen(false)} />}
    </header>
  );
}
