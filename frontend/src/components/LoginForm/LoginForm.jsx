import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdOutlineEmail, MdOutlineLock } from 'react-icons/md';
import { LuEye, LuEyeClosed } from 'react-icons/lu';

import { useAuth } from '../../context/AuthContext.jsx';
import { useLogin } from '../../hooks/useLogin.js';
import {
  createLoginCredentials,
  hasLoginFormErrors,
  validateLoginForm,
} from '../../utils/loginFormValidation.js';

import './LoginForm.css';

function getAuthenticatedUserFromLoginResponse(responseData) {
  const candidate = responseData?.user ?? responseData;

  if (!candidate || typeof candidate !== 'object') {
    return null;
  }

  if (!candidate.email && !candidate.name && !candidate.role) {
    return null;
  }

  return candidate;
}

export function LoginForm() {
  const navigate = useNavigate();

  const {
    restoreSession,
    setAuthenticatedUser,
  } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [errors, setErrors] = useState({});

  const {
    clearLoginError,
    isLoading,
    loginError,
    submitLogin,
  } = useLogin();

  async function handleSubmit(event) {
    event.preventDefault();

    const nextErrors = validateLoginForm({
      email,
      password,
    });

    setErrors(nextErrors);

    if (hasLoginFormErrors(nextErrors)) {
      return;
    }

    const credentials = createLoginCredentials({
      email,
      password,
    });

    const result = await submitLogin(credentials);

    if (!result.success) {
      return;
    }

    const authenticatedUser =
      getAuthenticatedUserFromLoginResponse(result.data);

    if (authenticatedUser) {
      setAuthenticatedUser(authenticatedUser);
    } else {
      await restoreSession({
        force: true,
      });
    }

    navigate('/panel', {
      replace: true,
    });
  }

  function handlePasswordVisibility() {
    setIsPasswordVisible((currentValue) => !currentValue);
  }

  function handleEmailChange(event) {
    setEmail(event.target.value);
    clearLoginError();

    if (errors.email) {
      setErrors((prevErrors) => ({
        ...prevErrors,
        email: undefined,
      }));
    }
  }

  function handlePasswordChange(event) {
    setPassword(event.target.value);
    clearLoginError();

    if (errors.password) {
      setErrors((prevErrors) => ({
        ...prevErrors,
        password: undefined,
      }));
    }
  }

  return (
    <form
      className="login-form"
      onSubmit={handleSubmit}
      noValidate
    >
      <div className="login-field">
        <label htmlFor="login-email">
          Correo electrónico
        </label>

        <div className="login-input-wrapper">
          <MdOutlineEmail
            className="login-input-icon"
            aria-hidden="true"
          />

          <input
            id="login-email"
            name="email"
            type="email"
            placeholder="correo@gmail.com"
            value={email}
            autoComplete="email"
            required
            aria-invalid={Boolean(errors.email)}
            aria-describedby={
              errors.email
                ? 'login-email-error'
                : undefined
            }
            onChange={handleEmailChange}
          />
        </div>

        <p
          className="login-error-message"
          id="login-email-error"
        >
          {errors.email || ''}
        </p>
      </div>

      <div className="login-field login-field-password">
        <label htmlFor="login-password">
          Contraseña
        </label>

        <div className="login-input-wrapper">
          <MdOutlineLock
            className="login-input-icon"
            aria-hidden="true"
          />

          <input
            id="login-password"
            name="password"
            type={
              isPasswordVisible
                ? 'text'
                : 'password'
            }
            placeholder="••••••••••••"
            value={password}
            autoComplete="current-password"
            required
            aria-invalid={Boolean(errors.password)}
            aria-describedby={
              errors.password
                ? 'login-password-error'
                : undefined
            }
            onChange={handlePasswordChange}
          />

          <button
            className="password-toggle"
            type="button"
            aria-label={
              isPasswordVisible
                ? 'Ocultar contraseña'
                : 'Mostrar contraseña'
            }
            onClick={handlePasswordVisibility}
          >
            {isPasswordVisible ? (
              <LuEyeClosed aria-hidden="true" />
            ) : (
              <LuEye aria-hidden="true" />
            )}
          </button>
        </div>

        <div className="password-footer-row">
          <p
            className="login-error-message"
            id="login-password-error"
          >
            {errors.password || ''}
          </p>

          <Link
            className="forgot-password-link"
            to="/recover-password"
          >
            ¿Has olvidado tu contraseña?
          </Link>
        </div>
      </div>

      {loginError && (
        <p
          className="login-error-message"
          role="alert"
        >
          {loginError}
        </p>
      )}

      <button
        className="login-submit"
        type="submit"
        disabled={isLoading}
        aria-busy={isLoading}
      >
        {isLoading
          ? 'Iniciando sesión…'
          : 'Iniciar sesión'}
      </button>

      <p className="request-account-text">
        ¿No tiene cuenta aún?{' '}
        <Link
          className="request-account-link"
          to="/solicita-cuenta"
        >
          Solicita acceso
        </Link>
      </p>
    </form>
  );
}