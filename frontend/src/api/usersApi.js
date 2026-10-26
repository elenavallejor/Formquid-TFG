import { apiClient } from './apiClient.js';

/** Gestiona las operaciones sobre usuarios, cuenta propia y roles. */
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

export function transferSurveyOwnership(surveyId, newOwnerId, currentPassword) {
  return apiClient(`/users/surveys/${surveyId}/transfer-ownership`, {
    method: 'POST',
    body: { newOwnerId, currentPassword },
  });
}

export function getUsers({
  page = 0,
  size = 20,
  search = '',
  searchFields = [],
  sortOrder = 'NAME_ASC',
} = {}) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortOrder,
    searchFields: searchFields.length > 0 ? searchFields.join(',') : 'NONE',
  });
  if (search.trim()) params.set('search', search.trim());

  return apiClient(`/users?${params.toString()}`, { method: 'GET' });
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
