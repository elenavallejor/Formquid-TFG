import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { MdOutlineLock } from 'react-icons/md';

import { confirmPasswordReset } from '../../../api/authApi.js';
import { AuthPageLayout } from '../../../components/AuthPageLayout/AuthPageLayout.jsx';
import { PasswordVisibilityIcon } from '../../../components/PasswordVisibilityIcon/PasswordVisibilityIcon.jsx';

import '../../../components/LoginForm/LoginForm.css';
import '../PasswordRecoveryPage/PasswordRecoveryPage.css';

function PasswordField({ id, label, value, onChange, visible, onToggle, error, autoComplete }) {
  return (
    <div className="login-field">
      <label htmlFor={id}>{label}</label>
      <div className="login-input-wrapper">
        <MdOutlineLock className="login-input-icon" aria-hidden="true" />
        <input id={id} name={id} type={visible ? 'text' : 'password'} value={value}
          onChange={onChange} autoComplete={autoComplete} required minLength={8} maxLength={120}
          aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined}
          placeholder="••••••••••••" />
        <button className="password-toggle" type="button"
          aria-label={visible ? `Ocultar ${label.toLowerCase()}` : `Mostrar ${label.toLowerCase()}`}
          aria-pressed={visible} onClick={onToggle}>
          <PasswordVisibilityIcon visible={visible} />
        </button>
      </div>
      <p className="login-error-message" id={`${id}-error`}>{error || ''}</p>
    </div>
  );
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [token] = useState(() => searchParams.get('token') || '');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [errors, setErrors] = useState({});
  const [requestError, setRequestError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isComplete, setIsComplete] = useState(false);

  useEffect(() => {
    if (token) navigate('/restablecer-contrasena', { replace: true });
  }, [navigate, token]);

  async function handleSubmit(event) {
    event.preventDefault();
    if (isLoading) return;
    const nextErrors = {};
    if (password.length < 8 || password.length > 120) {
      nextErrors.password = 'La contraseña debe tener entre 8 y 120 caracteres.';
    }
    if (!confirmation) nextErrors.confirmation = 'Confirma tu nueva contraseña.';
    else if (password !== confirmation) nextErrors.confirmation = 'Las contraseñas no coinciden.';
    setErrors(nextErrors);
    setRequestError('');
    if (Object.keys(nextErrors).length) return;

    setIsLoading(true);
    try {
      await confirmPasswordReset(token, password);
      setPassword('');
      setConfirmation('');
      setIsComplete(true);
    } catch (error) {
      setRequestError(error.message || 'No se ha podido restablecer la contraseña.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <AuthPageLayout title="Restablecer contraseña" description="Elige una nueva contraseña para tu cuenta.">
      {!token || isComplete ? (
        <div className="login-form" role="status">
          <p className="password-recovery-notice">
            {isComplete ? 'Tu contraseña se ha actualizado. Ya puedes iniciar sesión.' : 'Este enlace no es válido. Solicita uno nuevo para restablecer tu contraseña.'}
          </p>
          <Link className="login-submit password-recovery-action" to={isComplete ? '/inicia-sesion' : '/recover-password'}>
            {isComplete ? 'Iniciar sesión' : 'Solicitar otro enlace'}
          </Link>
        </div>
      ) : (
        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <PasswordField id="reset-password" label="Nueva contraseña" value={password}
            onChange={(event) => { setPassword(event.target.value); setErrors((current) => ({ ...current, password: '' })); setRequestError(''); }}
            visible={showPassword} onToggle={() => setShowPassword((current) => !current)}
            error={errors.password} autoComplete="new-password" />
          <PasswordField id="reset-confirmation" label="Confirmar contraseña" value={confirmation}
            onChange={(event) => { setConfirmation(event.target.value); setErrors((current) => ({ ...current, confirmation: '' })); setRequestError(''); }}
            visible={showConfirmation} onToggle={() => setShowConfirmation((current) => !current)}
            error={errors.confirmation} autoComplete="new-password" />
          {requestError && <p className="login-error-message" role="alert">{requestError}</p>}
          <button className="login-submit" type="submit" disabled={isLoading} aria-busy={isLoading}>
            {isLoading ? 'Guardando…' : 'Guardar nueva contraseña'}
          </button>
          <p className="request-account-text"><Link className="request-account-link" to="/recover-password">Solicitar otro enlace</Link></p>
        </form>
      )}
    </AuthPageLayout>
  );
}
