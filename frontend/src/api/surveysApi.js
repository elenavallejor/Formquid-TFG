import { apiClient } from './apiClient.js';

// Encuestas públicas
export async function getPublicSurveys() {
  return apiClient('/public/surveys', { method: 'GET' });
}

export async function getPublicSurveyById(surveyId) {
  return apiClient(`/public/surveys/${surveyId}`, { method: 'GET' });
}

// Encuestas autenticadas
export async function getMySurveys() {
  return apiClient('/surveys', { method: 'GET' });
}

export async function getRegisteredSurveys() {
  return apiClient('/surveys/registered', { method: 'GET' });
}

export async function createSurvey(data) {
  return apiClient('/surveys', {
    method: 'POST',
    body: data,
  });
}

export async function getSurveyById(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'GET' });
}

export async function updateSurvey(surveyId, data) {
  return apiClient(`/surveys/${surveyId}`, {
    method: 'PUT',
    body: data,
  });
}

export async function updateSurveyLifecycle(surveyId, lifecycle) {
  return apiClient(`/surveys/${surveyId}/lifecycle`, {
    method: 'PATCH',
    body: { lifecycle },
  });
}

export async function deleteSurvey(surveyId) {
  return apiClient(`/surveys/${surveyId}`, { method: 'DELETE' });
}

// Administración de Encuestas
export async function getAllAdminSurveys() {
  return apiClient('/admin/surveys', { method: 'GET' });
}

export async function updateAdminSurveyLifecycle(surveyId, lifecycle) {
  return apiClient(`/admin/surveys/${surveyId}/lifecycle`, {
    method: 'PATCH',
    body: { lifecycle },
  });
}

export async function deleteAdminSurvey(surveyId) {
  return apiClient(`/admin/surveys/${surveyId}`, { method: 'DELETE' });
}

export async function getAdminSurveysSummary() {
  return apiClient('/admin/surveys/summary', { method: 'GET' });
}