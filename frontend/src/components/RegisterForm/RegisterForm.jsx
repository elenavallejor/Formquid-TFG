import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdOutlineEmail, MdOutlineLock, MdOutlinePerson, MdOutlineBusiness, MdOutlineEdit, MdCheckCircleOutline } from 'react-icons/md';
import { LuEye, LuEyeClosed } from 'react-icons/lu';

import { createRegistrationRequest } from '../../api/registrationRequestsApi.js';
import './RegisterForm.css';

const INITIAL_FORM_DATA = {
  name: '',
  email: '',
  gender: '',
  institution: '',
  motivation: '',
  password: '',
  confirmPassword: '',
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateRegisterForm(data) {
  const errors = {};

  if (!data.name.trim()) {
    errors.name = 'El nombre completo es obligatorio.';
  }

  if (!data.email.trim()) {
    errors.email = 'El correo electrónico es obligatorio.';
  } else if (!EMAIL_PATTERN.test(data.email)) {
    errors.email = 'Introduce un correo electrónico válido.';
  }

  if (!data.gender) {
    errors.gender = 'Selecciona una opción de género.';
  }

  if (!data.institution.trim()) {
    errors.institution = 'La institución es obligatoria.';
  }

  if (!data.motivation.trim()) {
    errors.motivation = 'Explica brevemente tu motivación.';
  }

  if (!data.password) {
    errors.password = 'La contraseña es obligatoria.';
  } else if (data.password.length < 8) {
    errors.password = 'La contraseña debe tener al menos 8 caracteres.';
  }

  if (!data.confirmPassword) {
    errors.confirmPassword = 'Confirma tu contraseña.';
  } else if (data.password !== data.confirmPassword) {
    errors.confirmPassword = 'Las contraseñas no coinciden.';
  }

  return errors;
}

function hasRegisterFormErrors(errors) {
  return Object.keys(errors).length > 0;
}

export function RegisterForm() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState(INITIAL_FORM_DATA);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false); // Estado de confirmación
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));

    if (errors[name]) {
      setErrors((currentErrors) => ({
        ...currentErrors,
        [name]: undefined,
      }));
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError('');

    const nextErrors = validateRegisterForm(formData);
    setErrors(nextErrors);

    if (hasRegisterFormErrors(nextErrors)) {
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        name: formData.name.trim(),
        email: formData.email.trim(),
        gender: formData.gender,
        institution: formData.institution.trim(),
        motivation: formData.motivation.trim(),
        password: formData.password,
      };

      await createRegistrationRequest(payload);
      
      setIsSubmitted(true);
    } catch (error) {
      setSubmitError(
        error.message || 'No se ha podido enviar la solicitud. Inténtalo más tarde.'
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  // Vista de confirmación tras envío exitoso
  if (isSubmitted) {
    return (
      <div className="register-success-container">
        <MdCheckCircleOutline className="register-success-icon" aria-hidden="true" />
        <h3 className="register-success-title">¡Solicitud enviada con éxito!</h3>
        <p className="register-success-message">
          Hemos recibido tus datos correctamente. Un administrador revisará tu perfil y recibirás una respuesta en tu correo electrónico.
        </p>
        <button
          type="button"
          className="register-submit"
          onClick={() => navigate('/')}
        >
          Volver a la página principal
        </button>
      </div>
    );
  }

  return (
    <form className="register-form" onSubmit={handleSubmit} noValidate>
      {submitError && (
        <p className="register-submit-error" role="alert">
          {submitError}
        </p>
      )}

      <div className="register-field">
        <label htmlFor="register-name">Nombre completo</label>
        <div className="register-input-wrapper">
          <MdOutlinePerson className="register-input-icon" aria-hidden="true" />
          <input
            id="register-name"
            name="name"
            type="text"
            placeholder="Tu nombre completo"
            value={formData.name}
            autoComplete="name"
            required
            aria-invalid={Boolean(errors.name)}
            aria-describedby={errors.name ? 'register-name-error' : undefined}
            onChange={handleChange}
          />
        </div>
        <p className="register-error-message" id="register-name-error">
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
            autoComplete="email"
            required
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'register-email-error' : undefined}
            onChange={handleChange}
          />
        </div>
        <p className="register-error-message" id="register-email-error">
          {errors.email || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-gender">Género</label>
        <div className="register-input-wrapper">
          <select
            id="register-gender"
            name="gender"
            value={formData.gender}
            required
            aria-invalid={Boolean(errors.gender)}
            aria-describedby={errors.gender ? 'register-gender-error' : undefined}
            onChange={handleChange}
          >
            <option value="">Selecciona una opción</option>
            <option value="FEMALE">Mujer</option>
            <option value="MALE">Hombre</option>
            <option value="OTHER">Otro</option>
          </select>
        </div>
        <p className="register-error-message" id="register-gender-error">
          {errors.gender || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-institution">Institución</label>
        <div className="register-input-wrapper">
          <MdOutlineBusiness className="register-input-icon" aria-hidden="true" />
          <input
            id="register-institution"
            name="institution"
            type="text"
            placeholder="Universidad, centro o entidad"
            value={formData.institution}
            autoComplete="organization"
            required
            aria-invalid={Boolean(errors.institution)}
            aria-describedby={
              errors.institution ? 'register-institution-error' : undefined
            }
            onChange={handleChange}
          />
        </div>
        <p className="register-error-message" id="register-institution-error">
          {errors.institution || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-motivation">Motivación</label>
        <div className="register-input-wrapper register-textarea-wrapper">
          <MdOutlineEdit className="register-input-icon register-textarea-icon" aria-hidden="true" />
          <textarea
            id="register-motivation"
            name="motivation"
            rows="3"
            placeholder="Cuéntanos brevemente por qué deseas formar parte de nuestro equipo como editor..."
            value={formData.motivation}
            required
            aria-invalid={Boolean(errors.motivation)}
            aria-describedby={
              errors.motivation ? 'register-motivation-error' : undefined
            }
            onChange={handleChange}
          />
        </div>
        <p className="register-error-message" id="register-motivation-error">
          {errors.motivation || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-password">Contraseña</label>
        <div className="register-input-wrapper">
          <MdOutlineLock className="register-input-icon" aria-hidden="true" />
          <input
            id="register-password"
            name="password"
            type={isPasswordVisible ? 'text' : 'password'}
            placeholder="Mínimo 8 caracteres"
            value={formData.password}
            autoComplete="new-password"
            required
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? 'register-password-error' : undefined}
            onChange={handleChange}
          />
          <button
            className="password-toggle"
            type="button"
            aria-label={isPasswordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            onClick={() => setIsPasswordVisible((currentValue) => !currentValue)}
          >
            {isPasswordVisible ? (
              <LuEyeClosed aria-hidden="true" />
            ) : (
              <LuEye aria-hidden="true" />
            )}
          </button>
        </div>
        <p className="register-error-message" id="register-password-error">
          {errors.password || ''}
        </p>
      </div>

      <div className="register-field">
        <label htmlFor="register-confirm-password">Confirmar contraseña</label>
        <div className="register-input-wrapper">
          <MdOutlineLock className="register-input-icon" aria-hidden="true" />
          <input
            id="register-confirm-password"
            name="confirmPassword"
            type={isConfirmPasswordVisible ? 'text' : 'password'}
            placeholder="Repite la contraseña"
            value={formData.confirmPassword}
            autoComplete="new-password"
            required
            aria-invalid={Boolean(errors.confirmPassword)}
            aria-describedby={
              errors.confirmPassword ? 'register-confirm-password-error' : undefined
            }
            onChange={handleChange}
          />
          <button
            className="password-toggle"
            type="button"
            aria-label={
              isConfirmPasswordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'
            }
            onClick={() =>
              setIsConfirmPasswordVisible((currentValue) => !currentValue)
            }
          >
            {isConfirmPasswordVisible ? (
              <LuEyeClosed aria-hidden="true" />
            ) : (
              <LuEye aria-hidden="true" />
            )}
          </button>
        </div>
        <p className="register-error-message" id="register-confirm-password-error">
          {errors.confirmPassword || ''}
        </p>
      </div>

      <button className="register-submit" type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Enviando solicitud...' : 'Enviar solicitud'}
      </button>

      <p className="register-login-text">
        ¿Ya tienes cuenta?{' '}
        <Link className="register-login-link" to="/inicia-sesion">
          Inicia sesión
        </Link>
      </p>
    </form>
  );
}