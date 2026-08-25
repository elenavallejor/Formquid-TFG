import { apiClient } from './apiClient.js';

/**
 * Gestiona las operaciones de autenticación y restauración de la sesión del usuario.
 */
export function login(credentials) {
  return apiClient('/auth/login', {
    method: 'POST',
    body: credentials,
  });
}

export function getCurrentUser() {
  return apiClient('/users/me', { method: 'GET' });
}

export function logout() {
  return apiClient('/auth/logout', { method: 'POST' });
}
