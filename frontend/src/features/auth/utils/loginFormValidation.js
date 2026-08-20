const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateLoginForm({ email, password }) {
  const errors = {};
  const normalizedEmail = email.trim();

  if (!normalizedEmail) {
    errors.email = 'Introduce tu correo electrónico.';
  } else if (!EMAIL_PATTERN.test(normalizedEmail)) {
    errors.email = 'Introduce un correo electrónico válido.';
  }

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