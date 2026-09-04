import { apiClient } from './apiClient.js';

/**
 * Gestiona las operaciones sobre usuarios, cuenta propia, roles y transferencia de propiedad.
 */
export function getMe() {
  return apiClient('/users/me', { method: 'GET' });
}

export function updateMyAccount(data) {
  return apiClient('/users/me/account', {
    method: 'PATCH',
    body: data,
  });
}

export function deleteMyAccount(data) {
  return apiClient('/users/me', {
    method: 'DELETE',
    body: data,
  });
}

export function getUsers(page = 0, size = 20) {
  return apiClient(`/users?page=${page}&size=${size}`, { method: 'GET' });
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