import { useEffect } from 'react';
import { useLocation, useRouteError } from 'react-router-dom';

import errorBg from '../../../assets/images/error-page-background.png';

import './ErrorPage.css';

/**
 * Muestra una página común para errores de navegación, 
 * permisos y errores internos.
 */
export function ErrorPage({ code, title, message }) {
  const routeError = useRouteError();
  const location = useLocation();

  const errorCode = String(
    code ||
    routeError?.status ||
    (routeError ? 500 : 404),
  );

  const defaultTitles = {
    403: 'Acceso no permitido',
    404: 'Página no encontrada',
    500: 'Error interno',
  };

  const defaultMessages = {
    403: 'No tienes permisos para acceder a esta sección de FormQuid.',
    404: `No se ha encontrado la URL solicitada ${location.pathname}.`,
    500: 'Se ha producido un error interno. Inténtalo de nuevo más tarde.',
  };

  const errorTitle =
    title ||
    defaultTitles[errorCode] ||
    'Ha ocurrido un error';

  const errorMessage =
    message ||
    defaultMessages[errorCode] ||
    'Se ha producido un error inesperado.';

  useEffect(() => {
    document.title = `${errorTitle} | FormQuid`;
  }, [errorTitle]);

  return (
    <main className="error-page">
      <div className="error-page-container">
        <div className="error-page-text">
          <h1 className="sr-only">{errorTitle}</h1>

          <p className="error-page-logo" aria-hidden="true">FormQuid</p>

          <p className="error-page-status">
            <strong>{errorCode}.</strong> {errorTitle}
          </p>

          <p className="error-page-description">
            {errorMessage}
          </p>
        </div>

        <div className="error-page-illustration">
          <img
            src={errorBg}
            alt=""
            aria-hidden="true"
            className="error-page-image"
          />
        </div>
      </div>
    </main>
  );
}
