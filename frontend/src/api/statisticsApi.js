import { apiClient } from './apiClient.js';

/**
 * Gestiona la consulta de estadísticas generales y por pregunta de una encuesta.
 */
export function getSurveyStatistics(surveyId) {
  return apiClient(`/surveys/${surveyId}/statistics`, {
    method: 'GET',
  });
}

export function getQuestionStatistics(surveyId, questionId) {
  return apiClient(`/surveys/${surveyId}/statistics/questions/${questionId}`, {
    method: 'GET',
  });
}
