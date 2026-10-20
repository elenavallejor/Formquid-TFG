import { apiClient } from './apiClient.js';

/** Consulta las estadísticas generales de una encuesta. */
export function getSurveyStatistics(surveyId) {
  return apiClient(`/surveys/${surveyId}/statistics`, {
    method: 'GET',
  });
}
