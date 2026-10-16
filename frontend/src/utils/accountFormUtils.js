/**
 * Define límites y validaciones del formulario de configuración de cuenta.
 */
export const ACCOUNT_FORM_LIMITS = {
  name: 150,
  institution: 200,
  country: 100,
  email: 320,
  passwordMin: 8,
  passwordMax: 120,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export function hasPersonalChanges(personalData, initialPersonalData) {
  return (
    personalData.nombre.trim() !== initialPersonalData.nombre.trim() ||
    personalData.institucion.trim() !== initialPersonalData.institucion.trim() ||
    personalData.pais.trim() !== initialPersonalData.pais.trim()
  );
}

export function hasFormChanges(personalData, initialPersonalData, passwordData, emailData) {
  const isPasswordEntered = Boolean(passwordData.newPassword || passwordData.confirmPassword);
  const isEmailEntered = Boolean(emailData.newEmail || emailData.confirmNewEmail);

  return hasPersonalChanges(personalData, initialPersonalData) || isPasswordEntered || isEmailEntered;
}

export function validatePersonalInfo(data) {
  const errors = {};
  const name = (data.nombre || '').trim();
  const institution = (data.institucion || '').trim();
  const country = (data.pais || '').trim();

  if (!name) {
    errors.nombre = 'Introduce tu nombre completo.';
  } else if (name.length > ACCOUNT_FORM_LIMITS.name) {
    errors.nombre = `El nombre no puede superar los ${ACCOUNT_FORM_LIMITS.name} caracteres.`;
  }

  if (!institution) {
    errors.institucion = 'Introduce tu institución.';
  } else if (institution.length > ACCOUNT_FORM_LIMITS.institution) {
    errors.institucion = `La institución no puede superar los ${ACCOUNT_FORM_LIMITS.institution} caracteres.`;
  }

  if (!country) {
    errors.pais = 'Introduce tu país.';
  } else if (country.length > ACCOUNT_FORM_LIMITS.country) {
    errors.pais = `El país no puede superar los ${ACCOUNT_FORM_LIMITS.country} caracteres.`;
  }

  return errors;
}

export function validatePasswordChange(data) {
  const errors = {};
  const { newPassword, confirmPassword } = data;
  const isAttemptingChange = Boolean(newPassword || confirmPassword);

  if (!isAttemptingChange) {
    return errors;
  }

  if (!newPassword) {
    errors.newPassword = 'Introduce la nueva contraseña.';
  } else if (newPassword.length < ACCOUNT_FORM_LIMITS.passwordMin) {
    errors.newPassword = `La contraseña debe tener al menos ${ACCOUNT_FORM_LIMITS.passwordMin} caracteres.`;
  } else if (newPassword.length > ACCOUNT_FORM_LIMITS.passwordMax) {
    errors.newPassword = `La contraseña no puede superar los ${ACCOUNT_FORM_LIMITS.passwordMax} caracteres.`;
  }

  if (!confirmPassword) {
    errors.confirmPassword = 'Confirma la nueva contraseña.';
  } else if (newPassword !== confirmPassword) {
    errors.confirmPassword = 'Las contraseñas no coinciden.';
  }

  return errors;
}

export function validateEmailChange(data) {
  const errors = {};
  const currentEmail = (data.currentEmail || '').trim().toLowerCase();
  const newEmail = (data.newEmail || '').trim().toLowerCase();
  const confirmNewEmail = (data.confirmNewEmail || '').trim().toLowerCase();
  const isAttemptingChange = Boolean(newEmail || confirmNewEmail);

  if (!isAttemptingChange) {
    return errors;
  }

  if (!newEmail) {
    errors.newEmail = 'Introduce el nuevo correo electrónico.';
  } else if (!EMAIL_PATTERN.test(newEmail)) {
    errors.newEmail = 'Introduce un correo electrónico válido.';
  } else if (newEmail.length > ACCOUNT_FORM_LIMITS.email) {
    errors.newEmail = `El correo no puede superar los ${ACCOUNT_FORM_LIMITS.email} caracteres.`;
  } else if (newEmail === currentEmail) {
    errors.newEmail = 'El nuevo correo debe ser distinto al actual.';
  }

  if (!confirmNewEmail) {
    errors.confirmNewEmail = 'Confirma el nuevo correo electrónico.';
  } else if (newEmail !== confirmNewEmail) {
    errors.confirmNewEmail = 'Los correos no coinciden.';
  }

  return errors;
}

export function hasAccountFormErrors(errors) {
  return Object.values(errors).some(Boolean);
}
