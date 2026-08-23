import { apiClient } from './apiClient.js';

/**
 * Inicia sesión en la plataforma.
 */
export async function login(credentials) {
  return apiClient('/auth/login', {
    method: 'POST',
    body: credentials,
  });
}

/**
 * Cierra la sesión activa.
 */
export async function logout() {
  return apiClient('/auth/logout', {
    method: 'POST',
  });
}

/**
 * Obtiene el perfil del usuario autenticado (/users/me).
 */
export async function getCurrentUser() {
  return apiClient('/users/me', {
    method: 'GET',
  });
}