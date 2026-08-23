import { apiClient } from './apiClient.js';

export async function getSurveyStatistics(surveyId) {
  return apiClient(`/surveys/${surveyId}/statistics`, { method: 'GET' });
}

export async function getQuestionStatistics(surveyId, questionId) {
  return apiClient(`/surveys/${surveyId}/statistics/questions/${questionId}`, {
    method: 'GET',
  });
}