import { apiClient } from './apiClient.js';

/**
 * Gestiona las operaciones públicas, privadas y administrativas relacionadas con las encuestas.
 */
export function getPublicSurveys() {
  return apiClient('/public/surveys', { method: 'GET' });
}

export function getPublicSurveyById(surveyId) {
  return apiClient(`/public/surveys/${surveyId}`, { method: 'GET' });
}

export function accessPasswordProtectedSurvey(surveyId, accessPassword) {
  return apiClient(`/public/surveys/${surveyId}/access`, {
    method: 'POST',
    body: { accessPassword },
  });
}

export function getMySurveys({
  search = '',
  lifecycleStatus = 'ALL',
  sortOrder = 'RECENT',
  page = 0,
  size = 8,
  signal,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) {
    params.set('search', normalizedSearch);
  }

  if (lifecycleStatus !== 'ALL') {
    params.set('lifecycleStatus', lifecycleStatus);
  }

  params.set(
    'sort',
    `updatedAt,${sortOrder === 'OLDEST' ? 'asc' : 'desc'}`,
  );
  params.set('page', String(page));
  params.set('size', String(size));

  return apiClient(`/surveys?${params.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function getRegisteredSurveys() {
  return apiClient('/surveys/registered', { method: 'GET' });
}

export function getSurveyDirectory({
  search = '',
  lifecycleStatus = 'ALL',
  sortOrder = 'RECENT',
  page = 0,
  size = 9,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  if (lifecycleStatus !== 'ALL') params.set('lifecycleStatus', lifecycleStatus);
  params.set('sort', `updatedAt,${sortOrder === 'OLDEST' ? 'asc' : 'desc'}`);
  params.set('page', String(page));
  params.set('size', String(size));

  return apiClient(`/surveys/directory?${params.toString()}`, { method: 'GET' });
}

export function createSurvey(data) {
  return apiClient('/surveys', {
    method: 'POST',
    body: data,
  });
}

export function createSurveyFromTemplate(templateId, data) {
  return apiClient(`/surveys/from-template/${templateId}`, {
    method: 'POST',
    body: data,
  });
}

export function getSurveyById(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'GET' });
}

export function updateSurvey(surveyId, data) {
  return apiClient(`/surveys/${surveyId}`, {
    method: 'PUT',
    body: data,
  });
}

export function updateSurveyLifecycle(surveyId, lifecycleStatus) {
  return apiClient(`/surveys/${surveyId}/lifecycle`, {
    method: 'PATCH',
    body: { lifecycleStatus },
  });
}

export function deleteSurvey(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'DELETE' });
}

export function getAdminSurveys({
  search = '',
  lifecycleStatus = 'ALL',
  visibility = 'ALL',
  page = 0,
  size = 9,
  signal,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  if (lifecycleStatus !== 'ALL') params.set('lifecycleStatus', lifecycleStatus);
  if (visibility !== 'ALL') params.set('visibility', visibility);
  params.set('page', String(page));
  params.set('size', String(size));

  return apiClient(`/admin/surveys?${params.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function updateAdminSurveyLifecycle(surveyId, lifecycleStatus) {
  return apiClient(`/admin/surveys/${surveyId}/lifecycle`, {
    method: 'PATCH',
    body: { lifecycleStatus },
  });
}

export function updateAdminSurveyVisibility(surveyId, visibility) {
  return apiClient(`/admin/surveys/${surveyId}/visibility`, {
    method: 'PATCH',
    body: { visibility },
  });
}

export function deleteAdminSurvey(surveyId) {
  return apiClient(`/admin/surveys/${surveyId}`, { method: 'DELETE' });
}
