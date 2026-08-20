export const INITIAL_REGISTER_FORM_DATA = {
  name: '',
  email: '',
  gender: '',
  institution: '',
  motivation: '',
  password: '',
  confirmPassword: '',
};

export const REGISTER_FORM_LIMITS = {
  name: 150,
  email: 320,
  institution: 200,
  motivation: 1000,
  password: 128,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const VALID_GENDERS = new Set(['FEMALE', 'MALE', 'OTHER']);

/**
 * Valida los datos del formulario de solicitud de registro.
 *
 * La validación del frontend mejora la experiencia de usuario, pero
 * no sustituye la validación obligatoria del backend.
 */
export function validateRegisterForm(formData) {
  const errors = {};

  const name = formData.name.trim();
  const email = formData.email.trim();
  const institution = formData.institution.trim();
  const motivation = formData.motivation.trim();

  if (!name) {
    errors.name = 'Introduce tu nombre completo.';
  } else if (name.length > REGISTER_FORM_LIMITS.name) {
    errors.name = 'El nombre no puede superar los 150 caracteres.';
  }

  if (!email) {
    errors.email = 'Introduce tu correo electrónico.';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Introduce un correo electrónico válido.';
  } else if (email.length > REGISTER_FORM_LIMITS.email) {
    errors.email = 'El correo no puede superar los 320 caracteres.';
  }

  if (!VALID_GENDERS.has(formData.gender)) {
    errors.gender = 'Selecciona una opción válida.';
  }

  if (!institution) {
    errors.institution = 'Introduce tu institución.';
  } else if (institution.length > REGISTER_FORM_LIMITS.institution) {
    errors.institution = 'La institución no puede superar los 200 caracteres.';
  }

  if (!motivation) {
    errors.motivation = 'Escribe una breve motivación para solicitar el registro.';
  } else if (motivation.length > REGISTER_FORM_LIMITS.motivation) {
    errors.motivation = 'La motivación no puede superar los 1000 caracteres.';
  }

  if (!formData.password) {
    errors.password = 'Introduce una contraseña.';
  } else if (formData.password.length < 8) {
    errors.password = 'La contraseña debe tener al menos 8 caracteres.';
  } else if (formData.password.length > REGISTER_FORM_LIMITS.password) {
    errors.password = 'La contraseña no puede superar los 128 caracteres.';
  }

  if (!formData.confirmPassword) {
    errors.confirmPassword = 'Confirma tu contraseña.';
  } else if (formData.password !== formData.confirmPassword) {
    errors.confirmPassword = 'Las contraseñas no coinciden.';
  }

  return errors;
}

export function hasRegisterFormErrors(errors) {
  return Object.values(errors).some(Boolean);
}

/**
 * Crea el payload exacto que necesita el endpoint público de solicitud.
 *
 * No se envía passwordHash, createdAt ni updatedAt: eso pertenece al backend.
 */
export function createRegistrationRequestPayload(formData) {
  return {
    role: 'EDITOR',
    name: formData.name.trim(),
    email: formData.email.trim().toLowerCase(),
    gender: formData.gender,
    institution: formData.institution.trim(),
    motivation: formData.motivation.trim(),
    password: formData.password,
  };
}