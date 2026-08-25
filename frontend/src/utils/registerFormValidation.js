
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
  password: 120,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const VALID_GENDERS = new Set(['FEMALE', 'MALE', 'OTHER']);

/**
 * Centraliza los valores iniciales, límites, validación y 
 * construcción del payload del formulario de solicitud de registro.
 */
export function validateRegisterForm(formData) {
  const errors = {};
  const name = formData.name.trim();
  const email = formData.email.trim();
  const institution = formData.institution.trim();
  const motivation = formData.motivation.trim();

  if (!name) errors.name = 'Introduce tu nombre completo.';
  else if (name.length > REGISTER_FORM_LIMITS.name) errors.name = `El nombre no puede superar los ${REGISTER_FORM_LIMITS.name} caracteres.`;

  if (!email) errors.email = 'Introduce tu correo electrónico.';
  else if (!EMAIL_PATTERN.test(email)) errors.email = 'Introduce un correo electrónico válido.';
  else if (email.length > REGISTER_FORM_LIMITS.email) errors.email = `El correo no puede superar los ${REGISTER_FORM_LIMITS.email} caracteres.`;

  if (!VALID_GENDERS.has(formData.gender)) errors.gender = 'Selecciona una opción válida.';

  if (!institution) errors.institution = 'Introduce tu institución.';
  else if (institution.length > REGISTER_FORM_LIMITS.institution) errors.institution = `La institución no puede superar los ${REGISTER_FORM_LIMITS.institution} caracteres.`;

  if (!motivation) errors.motivation = 'Escribe una breve motivación para solicitar el registro.';
  else if (motivation.length > REGISTER_FORM_LIMITS.motivation) errors.motivation = `La motivación no puede superar los ${REGISTER_FORM_LIMITS.motivation} caracteres.`;

  if (!formData.password) errors.password = 'Introduce una contraseña.';
  else if (formData.password.length < 8) errors.password = 'La contraseña debe tener al menos 8 caracteres.';
  else if (formData.password.length > REGISTER_FORM_LIMITS.password) errors.password = `La contraseña no puede superar los ${REGISTER_FORM_LIMITS.password} caracteres.`;

  if (!formData.confirmPassword) errors.confirmPassword = 'Confirma tu contraseña.';
  else if (formData.password !== formData.confirmPassword) errors.confirmPassword = 'Las contraseñas no coinciden.';

  return errors;
}

export function hasRegisterFormErrors(errors) {
  return Object.values(errors).some(Boolean);
}

export function createRegistrationRequestPayload(formData) {
  return {
    name: formData.name.trim(),
    email: formData.email.trim().toLowerCase(),
    gender: formData.gender,
    institution: formData.institution.trim(),
    motivation: formData.motivation.trim(),
    password: formData.password,
  };
}