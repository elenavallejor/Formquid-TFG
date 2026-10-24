import { apiClient } from './apiClient.js';

/**
 * Gestiona las operaciones públicas, privadas y administrativas relacionadas con las encuestas.
 */
function getMainPageSurveys(path, {
  search = '',
  tag = '',
  categories = [],
  accessMode = 'ALL',
  page = 0,
  size = 9,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();
  const normalizedTag = tag.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  if (normalizedTag) params.set('tag', normalizedTag);
  categories.forEach((category) => params.append('category', category));
  if (accessMode !== 'ALL') params.set('accessMode', accessMode);
  params.set('page', String(page));
  params.set('size', String(size));

  return apiClient(`${path}?${params.toString()}`, { method: 'GET' });
}

export function getPublicSurveys(options) {
  return getMainPageSurveys('/public/surveys/page', options);
}

export function getPublicSurveyById(surveyId) {
  return apiClient(`/public/surveys/${surveyId}`, { method: 'GET' });
}

export function getRegisteredSurveyByCode(publicCode) {
  return apiClient(`/surveys/by-code/${publicCode}`, { method: 'GET' });
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
  accessModes = [],
  categories = [],
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

  accessModes.forEach((mode) => params.append('accessMode', mode));
  categories.forEach((category) => params.append('category', category));

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

export function getResultSurveys({
  search = '',
  searchFields = [],
  lifecycleStatuses = [],
  sortOrder = 'NEWEST',
  page = 0,
  size = 9,
  signal,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  params.set('searchFields', searchFields.length > 0 ? searchFields.join(',') : 'NONE');
  lifecycleStatuses.forEach((status) => params.append('lifecycleStatus', status));
  params.set('sortOrder', sortOrder);
  params.set('page', String(page));
  params.set('size', String(size));

  return apiClient(`/surveys/results?${params.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function getRegisteredSurveys(options) {
  return getMainPageSurveys('/surveys/registered/page', options);
}

export function getSurveyDirectory({
  search = '',
  lifecycleStatuses = [],
  sortOrder = 'RECENT',
  page = 0,
  size = 9,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  lifecycleStatuses.forEach((status) => params.append('lifecycleStatus', status));
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

export function previewSurveyImport(file) {
  const body = new FormData();
  body.append('file', file);
  return apiClient('/surveys/import/preview', {
    method: 'POST',
    body,
  });
}

export function createSurveyFromImport(survey, content) {
  return apiClient('/surveys/from-import', {
    method: 'POST',
    body: { survey, content },
  });
}

export function getSurveyById(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'GET' });
}

export function getSurveyForResults(surveyId) {
  return apiClient(`/surveys/${surveyId}/results`, { method: 'GET' });
}

export function updateSurvey(surveyId, data) {
  return apiClient(`/surveys/${surveyId}`, {
    method: 'PUT',
    body: data,
  });
}

export function deleteSurvey(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'DELETE' });
}

export function uploadSurveyCover(surveyId, file) {
  const body = new FormData();
  body.append('file', file);
  return apiClient(`/surveys/${surveyId}/cover`, { method: 'POST', body });
}

export function updateSurveyLifecycle(surveyId, lifecycleStatus) {
  return apiClient(`/surveys/${surveyId}/lifecycle`, {
    method: 'PATCH',
    body: { lifecycleStatus },
  });
}

export function getAdminSurveys({
  search = '',
  lifecycleStatuses = [],
  visibilities = [],
  page = 0,
  size = 9,
  signal,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) params.set('search', normalizedSearch);
  lifecycleStatuses.forEach((status) => params.append('lifecycleStatus', status));
  visibilities.forEach((visibility) => params.append('visibility', visibility));
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
