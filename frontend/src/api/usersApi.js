import { apiClient } from './apiClient.js';

export async function getMe() {
  return apiClient('/users/me', { method: 'GET' });
}

export async function getUsers() {
  return apiClient('/users', { method: 'GET' });
}

export async function updateUserRole(userId, role) {
  return apiClient(`/users/${userId}/role`, {
    method: 'PATCH',
    body: { role },
  });
}

export async function deleteUser(userId) {
  return apiClient(`/users/${userId}`, { method: 'DELETE' });
}

export async function transferSurveyOwnership(surveyId, newOwnerId) {
  return apiClient(`/users/surveys/${surveyId}/transfer-ownership`, {
    method: 'POST',
    body: { newOwnerId },
  });
}