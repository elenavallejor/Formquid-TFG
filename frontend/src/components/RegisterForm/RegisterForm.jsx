import { useState } from 'react';
import { Link } from 'react-router-dom';
import { MdBusiness, MdCheckCircleOutline, MdEditNote, MdLockOutline, MdOutlineEmail, MdPersonOutline, MdPublic } from 'react-icons/md';
import { LuEye, LuEyeClosed } from 'react-icons/lu';

import { createRegistrationRequest } from '../../api/registrationRequestsApi';
import { INITIAL_REGISTER_FORM_DATA, REGISTER_FORM_LIMITS, createRegistrationRequestPayload, hasRegisterFormErrors, validateRegisterForm } from '../../utils/registerFormValidation';

import './RegisterForm.css';

/**
 * Gestiona el formulario público de solicitud de registro, 
 * su validación y el envío de los datos al servidor.
 */
export function RegisterForm() {
  const [formData, setFormData] = useState(INITIAL_REGISTER_FORM_DATA);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));

    if (errors[name]) {
      setErrors((previous) => ({
        ...previous,
        [name]: '',
      }));
    }

    if (submitError) setSubmitError('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSubmitting) return;

    const validationErrors = validateRegisterForm(formData);
    setErrors(validationErrors);
    setSubmitError('');

    if (hasRegisterFormErrors(validationErrors)) return;

    try {
      setIsSubmitting(true);

      const payload = createRegistrationRequestPayload(formData);
      await createRegistrationRequest(payload);

      setFormData(INITIAL_REGISTER_FORM_DATA);
      setErrors({});
      setIsSuccess(true);
    } catch (error) {
      setSubmitError(
        error?.message ||
          'No se ha podido enviar la solicitud. Inténtalo de nuevo más tarde.',
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isSuccess) {
    return (
      <div className="register-success-container" role="status">
        <MdCheckCircleOutline
          className="register-success-icon"
          aria-hidden="true"
        />
        <h2 className="register-success-title">Solicitud enviada</h2>
        <p className="register-success-message">
          Tu solicitud se ha enviado correctamente. Recibirás una notificación
          cuando haya sido revisada.
        </p>
        <Link to="/inicia-sesion" className="register-login-link">
          Ir a iniciar sesión
        </Link>
      </div>
    );
  }

  return (
    <form className="register-form" onSubmit={handleSubmit} noValidate>
      <div className="register-field">
        <label htmlFor="register-name">Nombre completo</label>
        <div className="register-input-wrapper">
          <MdPersonOutline className="register-input-icon" aria-hidden="true" />
          <input
            id="register-name"
            name="name"
            type="text"
            placeholder="Tu nombre completo"
            value={formData.name}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.name}
            autoComplete="name"
            aria-invalid={Boolean(errors.name)}
            aria-describedby={errors.name ? 'register-name-error' : undefined}
          />
        </div>
        <p id="register-name-error" className="register-error-message">
          {errors.name || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-email">Correo electrónico</label>
        <div className="register-input-wrapper">
          <MdOutlineEmail className="register-input-icon" aria-hidden="true" />
          <input
            id="register-email"
            name="email"
            type="email"
            placeholder="tu@email.com"
            value={formData.email}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.email}
            autoComplete="email"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'register-email-error' : undefined}
          />
        </div>
        <p id="register-email-error" className="register-error-message">
          {errors.email || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-institution">
          Institución / Organización
        </label>
        <div className="register-input-wrapper">
          <MdBusiness className="register-input-icon" aria-hidden="true" />
          <input
            id="register-institution"
            name="institution"
            type="text"
            placeholder="Ej: Universidad de Granada"
            value={formData.institution}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.institution}
            autoComplete="organization"
            aria-invalid={Boolean(errors.institution)}
            aria-describedby={
              errors.institution ? 'register-institution-error' : undefined
            }
          />
        </div>
        <p id="register-institution-error" className="register-error-message">
          {errors.institution || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-country">País</label>
        <div className="register-input-wrapper">
          <MdPublic className="register-input-icon" aria-hidden="true" />
          <input
            id="register-country"
            name="country"
            type="text"
            value={formData.country}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.country}
            autoComplete="country-name"
            aria-invalid={Boolean(errors.country)}
            aria-describedby={errors.country ? 'register-country-error' : undefined}
          />
        </div>
        <p id="register-country-error" className="register-error-message">{errors.country || ''}</p>
      </div>

      <div className="register-field">
        <label htmlFor="register-motivation">Motivación</label>
        <div className="register-input-wrapper register-textarea-wrapper">
          <MdEditNote
            className="register-input-icon register-textarea-icon"
            aria-hidden="true"
          />
          <textarea
            id="register-motivation"
            name="motivation"
            placeholder="Cuéntanos brevemente por qué deseas formar parte como editor..."
            value={formData.motivation}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.motivation}
            rows="4"
            aria-invalid={Boolean(errors.motivation)}
            aria-describedby={
              errors.motivation ? 'register-motivation-error' : undefined
            }
          />
        </div>
        <p id="register-motivation-error" className="register-error-message">
          {errors.motivation || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-password">Contraseña</label>
        <div className="register-input-wrapper">
          <MdLockOutline className="register-input-icon" aria-hidden="true" />
          <input
            id="register-password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            value={formData.password}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.password}
            autoComplete="new-password"
            aria-invalid={Boolean(errors.password)}
            aria-describedby={
              errors.password ? 'register-password-error' : undefined
            }
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword((previous) => !previous)}
            aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={showPassword}
          >
            {showPassword ? (
              <LuEyeClosed aria-hidden="true" />
            ) : (
              <LuEye aria-hidden="true" />
            )}
          </button>
        </div>
        <p id="register-password-error" className="register-error-message">
          {errors.password || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-confirm-password">Confirmar contraseña</label>
        <div className="register-input-wrapper">
          <MdLockOutline className="register-input-icon" aria-hidden="true" />
          <input
            id="register-confirm-password"
            name="confirmPassword"
            type={showConfirmPassword ? 'text' : 'password'}
            value={formData.confirmPassword}
            onChange={handleChange}
            maxLength={REGISTER_FORM_LIMITS.password}
            autoComplete="new-password"
            aria-invalid={Boolean(errors.confirmPassword)}
            aria-describedby={
              errors.confirmPassword
                ? 'register-confirm-password-error'
                : undefined
            }
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowConfirmPassword((previous) => !previous)}
            aria-label={
              showConfirmPassword
                ? 'Ocultar confirmación de contraseña'
                : 'Mostrar confirmación de contraseña'
            }
            aria-pressed={showConfirmPassword}
          >
            {showConfirmPassword ? (
              <LuEyeClosed aria-hidden="true" />
            ) : (
              <LuEye aria-hidden="true" />
            )}
          </button>
        </div>
        <p
          id="register-confirm-password-error"
          className="register-error-message"
        >
          {errors.confirmPassword || ''}
        </p>
      </div>

      {submitError && (
        <p className="register-error-message" role="alert">
          {submitError}
        </p>
      )}

      <button
        type="submit"
        className="register-submit"
        disabled={isSubmitting}
      >
        {isSubmitting ? 'Enviando...' : 'Enviar solicitud'}
      </button>

      <p className="register-login-text">
        ¿Ya tienes cuenta?{' '}
        <Link to="/inicia-sesion" className="register-login-link">
          Inicia sesión
        </Link>
      </p>
    </form>
  );
}
