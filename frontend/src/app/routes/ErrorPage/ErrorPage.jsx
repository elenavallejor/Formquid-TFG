import { useLocation, useRouteError } from 'react-router-dom';
import errorBg from '../../../assets/images/error-page-background.png';
import './ErrorPage.css';

export function ErrorPage() {
  const routeError = useRouteError();
  const location = useLocation();

  const errorCode = routeError?.status || '404';

  const defaultMessage = `No se ha encontrado la URL solicitada ${location.pathname}..`;

  const errorMessage =
    routeError?.statusText ||
    routeError?.message ||
    defaultMessage;

  return (
    <main className="error-page">
      <div className="error-page-container">
        <div className="error-page-text">
          <h1 className="error-page-logo">FormQuid</h1>
          <p className="error-page-status">
            <strong>{errorCode}.</strong> Ha ocurrido un error.
          </p>
          <p className="error-page-description">{errorMessage}</p>
        </div>

        <div className="error-page-illustration">
          <img
            src={errorBg}
            alt="Ilustración de error"
            className="error-page-image"
          />
        </div>
      </div>
    </main>
  );
}