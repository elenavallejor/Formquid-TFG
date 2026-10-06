import { apiClient } from './apiClient.js';

/**
 * Gestiona el envío de participaciones públicas y autenticadas de las encuestas.
 */
export function submitPublicParticipation(surveyId, answersData) {
  return apiClient(`/public/surveys/${surveyId}/participations`, {
    method: 'POST',
    body: answersData,
  });
}

export function submitAuthenticatedParticipation(surveyId, answersData) {
  return apiClient(`/surveys/${surveyId}/participations`, {
    method: 'POST',
    body: answersData,
  });
}

export function getSurveyParticipations(surveyId, { page = 0, size = 10, signal } = {}) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return apiClient(`/surveys/${surveyId}/participations?${params.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function getSurveyParticipation(surveyId, participationId) {
  return apiClient(`/surveys/${surveyId}/participations/${participationId}`, {
    method: 'GET',
  });
}
