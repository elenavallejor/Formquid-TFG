import { apiClient } from './apiClient.js';

export async function submitPublicParticipation(surveyId, answersData) {
  return apiClient(`/public/surveys/${surveyId}/participations`, {
    method: 'POST',
    body: answersData,
  });
}

export async function submitAuthenticatedParticipation(surveyId, answersData) {
  return apiClient(`/surveys/${surveyId}/participations`, {
    method: 'POST',
    body: answersData,
  });
}