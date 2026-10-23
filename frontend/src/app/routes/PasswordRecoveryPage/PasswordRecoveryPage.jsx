import { useState } from 'react';
import { Link } from 'react-router-dom';
import { MdOutlineEmail } from 'react-icons/md';

import { requestPasswordReset } from '../../../api/authApi.js';
import { AuthPageLayout } from '../../../components/AuthPageLayout/AuthPageLayout.jsx';
import { validateEmail } from '../../../utils/loginFormValidation.js';

import '../../../components/LoginForm/LoginForm.css';
import './PasswordRecoveryPage.css';

export function PasswordRecoveryPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [requestError, setRequestError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (isLoading) return;
    const validationError = validateEmail(email);
    setError(validationError);
    setRequestError('');
    if (validationError) return;

    setIsLoading(true);
    try {
      await requestPasswordReset(email.trim().toLowerCase());
      setSent(true);
    } catch {
      setRequestError('No se ha podido enviar la solicitud. Inténtalo de nuevo.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <AuthPageLayout title="Recuperar contraseña" description="Introduce tu correo y te enviaremos un enlace para restablecerla.">
      {sent ? (
        <div className="login-form" role="status">
          <p className="password-recovery-notice">Si existe una cuenta con ese correo, recibirás un enlace para restablecer la contraseña. Comprueba también la carpeta de correo no deseado.</p>
          <Link className="login-submit password-recovery-action" to="/inicia-sesion">Volver a iniciar sesión</Link>
        </div>
      ) : (
        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <div className="login-field">
            <label htmlFor="recovery-email">Correo electrónico</label>
            <div className="login-input-wrapper">
              <MdOutlineEmail className="login-input-icon" aria-hidden="true" />
              <input id="recovery-email" name="email" type="email" autoComplete="email"
                placeholder="correo@gmail.com" value={email} required maxLength={320}
                aria-invalid={Boolean(error)} aria-describedby={error ? 'recovery-email-error' : undefined}
                onChange={(event) => { setEmail(event.target.value); setError(''); setRequestError(''); }} />
            </div>
            <p className="login-error-message" id="recovery-email-error">{error}</p>
          </div>
          {requestError && <p className="login-error-message" role="alert">{requestError}</p>}
          <button className="login-submit" type="submit" disabled={isLoading} aria-busy={isLoading}>
            {isLoading ? 'Enviando…' : 'Enviar enlace de recuperación'}
          </button>
          <p className="request-account-text"><Link className="request-account-link" to="/inicia-sesion">Volver a iniciar sesión</Link></p>
        </form>
      )}
    </AuthPageLayout>
  );
}
