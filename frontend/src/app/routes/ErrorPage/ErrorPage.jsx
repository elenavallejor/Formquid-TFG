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
    <main className="error-page-minimal">
      <div className="error-page-minimal-container">
        <div className="error-page-minimal-text">
          <h1 className="error-page-minimal-logo">FormQuid</h1>
          <p className="error-page-minimal-status">
            <strong>{errorCode}.</strong> Ha ocurrido un error.
          </p>
          <p className="error-page-minimal-description">{errorMessage}</p>
        </div>

        <div className="error-page-minimal-illustration">
          <img
            src={errorBg}
            alt="Ilustración de error"
            className="error-page-minimal-image"
          />
        </div>
      </div>
    </main>
  );
}