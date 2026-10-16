
export const INITIAL_REGISTER_FORM_DATA = {
  name: '',
  email: '',
  institution: '',
  country: '',
  motivation: '',
  password: '',
  confirmPassword: '',
};

export const REGISTER_FORM_LIMITS = {
  name: 150,
  email: 320,
  institution: 200,
  country: 100,
  motivation: 1000,
  password: 120,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Centraliza los valores iniciales, límites, validación y 
 * construcción del payload del formulario de solicitud de registro.
 */
export function validateRegisterForm(formData) {
  const errors = {};
  const name = formData.name.trim();
  const email = formData.email.trim();
  const institution = formData.institution.trim();
  const country = formData.country.trim();
  const motivation = formData.motivation.trim();

  if (!name) errors.name = 'Introduce tu nombre completo.';
  else if (name.length > REGISTER_FORM_LIMITS.name) errors.name = `El nombre no puede superar los ${REGISTER_FORM_LIMITS.name} caracteres.`;

  if (!email) errors.email = 'Introduce tu correo electrónico.';
  else if (!EMAIL_PATTERN.test(email)) errors.email = 'Introduce un correo electrónico válido.';
  else if (email.length > REGISTER_FORM_LIMITS.email) errors.email = `El correo no puede superar los ${REGISTER_FORM_LIMITS.email} caracteres.`;

  if (!institution) errors.institution = 'Introduce tu institución.';
  else if (institution.length > REGISTER_FORM_LIMITS.institution) errors.institution = `La institución no puede superar los ${REGISTER_FORM_LIMITS.institution} caracteres.`;

  if (!country) errors.country = 'Introduce tu país.';
  else if (country.length > REGISTER_FORM_LIMITS.country) errors.country = `El país no puede superar los ${REGISTER_FORM_LIMITS.country} caracteres.`;

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
    institution: formData.institution.trim(),
    country: formData.country.trim(),
    motivation: formData.motivation.trim(),
    password: formData.password,
  };
}
