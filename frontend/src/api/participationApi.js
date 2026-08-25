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
