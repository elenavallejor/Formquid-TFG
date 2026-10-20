import { describe, expect, it } from 'vitest';

import { ACCOUNT_FORM_LIMITS, hasAccountFormErrors, hasFormChanges, hasPersonalChanges, validateEmailChange, validatePasswordChange, validatePersonalInfo } from './accountFormUtils.js';

const personal = { nombre: 'Ana', institucion: 'Universidad', pais: 'España' };

describe('validación de la cuenta', () => {
  it('detecta cambios reales y omite espacios exteriores', () => {
    expect(hasPersonalChanges({ ...personal, nombre: ' Ana ' }, personal)).toBe(false);
    expect(hasPersonalChanges({ ...personal, pais: 'Portugal' }, personal)).toBe(true);
    expect(hasFormChanges(personal, personal, { newPassword: 'clave' }, {})).toBe(true);
    expect(hasFormChanges(personal, personal, {}, { confirmNewEmail: 'ana@example.com' })).toBe(true);
    expect(hasFormChanges(personal, personal, {}, {})).toBe(false);
  });

  it('valida campos obligatorios y límites de datos personales', () => {
    expect(validatePersonalInfo({})).toMatchObject({ nombre: expect.any(String), institucion: expect.any(String), pais: expect.any(String) });
    expect(validatePersonalInfo({ ...personal, nombre: 'x'.repeat(ACCOUNT_FORM_LIMITS.name + 1) })).toHaveProperty('nombre');
    expect(validatePersonalInfo({ ...personal, institucion: 'x'.repeat(ACCOUNT_FORM_LIMITS.institution + 1) })).toHaveProperty('institucion');
    expect(validatePersonalInfo({ ...personal, pais: 'x'.repeat(ACCOUNT_FORM_LIMITS.country + 1) })).toHaveProperty('pais');
    expect(validatePersonalInfo(personal)).toEqual({});
  });

  it('comprueba longitud y confirmación de la contraseña', () => {
    expect(validatePasswordChange({})).toEqual({});
    expect(validatePasswordChange({ confirmPassword: 'clave' })).toHaveProperty('newPassword');
    expect(validatePasswordChange({ newPassword: 'corta' })).toMatchObject({ newPassword: expect.any(String), confirmPassword: expect.any(String) });
    expect(validatePasswordChange({ newPassword: 'x'.repeat(121), confirmPassword: 'otra' })).toMatchObject({ newPassword: expect.any(String), confirmPassword: expect.any(String) });
    expect(validatePasswordChange({ newPassword: 'clave123', confirmPassword: 'clave123' })).toEqual({});
  });

  it('rechaza correos inválidos, repetidos o sin confirmación', () => {
    expect(validateEmailChange({ currentEmail: 'ana@example.com' })).toEqual({});
    expect(validateEmailChange({ currentEmail: 'ana@example.com', confirmNewEmail: 'otro@example.com' })).toHaveProperty('newEmail');
    expect(validateEmailChange({ newEmail: 'incorrecto' })).toHaveProperty('newEmail');
    expect(validateEmailChange({ currentEmail: 'ANA@example.com', newEmail: 'ana@EXAMPLE.com' })).toHaveProperty('newEmail');
    expect(validateEmailChange({ newEmail: 'otro@example.com' })).toHaveProperty('confirmNewEmail');
    expect(validateEmailChange({ newEmail: 'otro@example.com', confirmNewEmail: 'distinto@example.com' })).toHaveProperty('confirmNewEmail');
    expect(validateEmailChange({ currentEmail: 'ana@example.com', newEmail: 'otro@example.com', confirmNewEmail: 'OTRO@example.com' })).toEqual({});
    expect(hasAccountFormErrors({ email: 'Error' })).toBe(true);
    expect(hasAccountFormErrors({ email: '' })).toBe(false);
  });
});
