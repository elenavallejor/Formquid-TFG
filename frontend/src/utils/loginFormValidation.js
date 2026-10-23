/**
 * Centraliza la validación y construcción de las credenciales 
 * utilizadas en el inicio de sesión.
 */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(email) {
  const normalizedEmail = email.trim();
  if (!normalizedEmail) return 'Introduce tu correo electrónico.';
  if (!EMAIL_PATTERN.test(normalizedEmail) || normalizedEmail.length > 320) {
    return 'Introduce un correo electrónico válido.';
  }
  return '';
}

export function validateLoginForm({ email, password }) {
  const errors = {};
  const emailError = validateEmail(email);
  if (emailError) errors.email = emailError;

  if (!password) {
    errors.password = 'Introduce tu contraseña.';
  }

  return errors;
}

export function hasLoginFormErrors(errors) {
  return Object.values(errors).some(Boolean);
}

export function createLoginCredentials({ email, password }) {
  return {
    email: email.trim().toLowerCase(),
    password,
  };
}
