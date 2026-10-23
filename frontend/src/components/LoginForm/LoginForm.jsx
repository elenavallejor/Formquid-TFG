import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MdOutlineEmail, MdOutlineLock } from 'react-icons/md';

import { useAuth } from '../../context/useAuth.js';
import { PasswordVisibilityIcon } from '../PasswordVisibilityIcon/PasswordVisibilityIcon.jsx';
import { createLoginCredentials, hasLoginFormErrors, validateLoginForm } from '../../utils/loginFormValidation.js';

import './LoginForm.css';

/**
 * Valida las credenciales y delega la autenticación y actualización 
 * de sesión en AuthContext.
 */
const LOGIN_ERROR_MESSAGE = 'No se ha podido iniciar sesión. Comprueba tus credenciales e inténtalo de nuevo.';

export function LoginForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [loginError, setLoginError] = useState('');

  async function handleSubmit(event) {
    event.preventDefault();
    if (isLoading) return;

    const nextErrors = validateLoginForm({ email, password });
    setErrors(nextErrors);
    setLoginError('');

    if (hasLoginFormErrors(nextErrors)) return;

    try {
      setIsLoading(true);

      const credentials = createLoginCredentials({ email, password });
      await login(credentials);

      const previousLocation = location.state?.from;
      const destination = previousLocation
        ? `${previousLocation.pathname}${previousLocation.search || ''}${previousLocation.hash || ''}`
        : '/panel';

      navigate(destination, {
        replace: true,
        state: previousLocation ? { returningFromLogin: true } : null,
      });
    } catch {
      setLoginError(LOGIN_ERROR_MESSAGE);
    } finally {
      setIsLoading(false);
    }
  }

  function handleEmailChange(event) {
    setEmail(event.target.value);
    setLoginError('');

    if (errors.email) {
      setErrors((previous) => ({ ...previous, email: undefined }));
    }
  }

  function handlePasswordChange(event) {
    setPassword(event.target.value);
    setLoginError('');

    if (errors.password) {
      setErrors((previous) => ({ ...previous, password: undefined }));
    }
  }

  return (
    <form className="login-form" onSubmit={handleSubmit} noValidate>
      <div className="login-field">
        <label htmlFor="login-email">Correo electrónico</label>

        <div className="login-input-wrapper">
          <MdOutlineEmail className="login-input-icon" aria-hidden="true" />
          <input
            id="login-email"
            name="email"
            type="email"
            placeholder="correo@gmail.com"
            value={email}
            autoComplete="email"
            required
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'login-email-error' : undefined}
            onChange={handleEmailChange}
          />
        </div>

        <p className="login-error-message" id="login-email-error">
          {errors.email || ''}
        </p>
      </div>

      <div className="login-field login-field-password">
        <label htmlFor="login-password">Contraseña</label>

        <div className="login-input-wrapper">
          <MdOutlineLock className="login-input-icon" aria-hidden="true" />
          <input
            id="login-password"
            name="password"
            type={isPasswordVisible ? 'text' : 'password'}
            placeholder="••••••••••••"
            value={password}
            autoComplete="current-password"
            required
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? 'login-password-error' : undefined}
            onChange={handlePasswordChange}
          />

          <button
            className="password-toggle"
            type="button"
            aria-label={isPasswordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={isPasswordVisible}
            onClick={() => setIsPasswordVisible((current) => !current)}
          >
            <PasswordVisibilityIcon visible={isPasswordVisible} />
          </button>
        </div>

        <div className="password-footer-row">
          <p className="login-error-message" id="login-password-error">
            {errors.password || ''}
          </p>
          <Link className="forgot-password-link" to="/recover-password">
            ¿Has olvidado tu contraseña?
          </Link>
        </div>
      </div>

      {loginError && (
        <p className="login-error-message" role="alert">
          {loginError}
        </p>
      )}

      <button className="login-submit" type="submit" disabled={isLoading} aria-busy={isLoading}>
        {isLoading ? 'Iniciando sesión…' : 'Iniciar sesión'}
      </button>

      <p className="request-account-text">
        ¿No tienes cuenta aún?{' '}
        <Link className="request-account-link" to="/solicita-cuenta">
          Solicita acceso
        </Link>
      </p>
    </form>
  );
}
