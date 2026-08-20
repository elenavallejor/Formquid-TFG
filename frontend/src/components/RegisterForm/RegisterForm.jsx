import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MdOutlineEmail,
  MdOutlineLock,
  MdOutlinePerson,
  MdOutlineBusiness,
  MdOutlineEdit,
} from 'react-icons/md';
import { LuEye, LuEyeClosed } from 'react-icons/lu';

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


export function RegisterForm() {
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);
  const [errors, setErrors] = useState({});
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

  const nextErrors = validateRegisterForm(formData);
  setErrors(nextErrors);

  if (hasRegisterFormErrors(nextErrors)) {
    return;
  }

  const payload = createRegistrationRequestPayload(formData);
  const result = await submitRegistrationRequest(payload);

  if (result.success) {
    setFormData(INITIAL_REGISTER_FORM_DATA);
    setIsPasswordVisible(false);
    setIsConfirmPasswordVisible(false);
  }
}

  return (
    <form className="register-form" onSubmit={handleSubmit} noValidate>
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

      <button className="register-submit" type="submit">
        Enviar solicitud
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