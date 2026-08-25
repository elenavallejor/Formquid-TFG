import { apiClient } from './apiClient.js';

/**
 * Gestiona las operaciones sobre usuarios, roles y transferencia de propiedad de encuestas.
 */
export function getMe() {
  return apiClient('/users/me', { method: 'GET' });
}

export function getUsers() {
  return apiClient('/users', { method: 'GET' });
}

export function updateUserRole(userId, role) {
  return apiClient(`/users/${userId}/role`, {
    method: 'PATCH',
    body: { role },
  });
}

export function deleteUser(userId) {
  return apiClient(`/users/${userId}`, { method: 'DELETE' });
}

export function transferSurveyOwnership(surveyId, newOwnerId) {
  return apiClient(`/users/surveys/${surveyId}/transfer-ownership`, {
    method: 'POST',
    body: { newOwnerId },
  });
}
