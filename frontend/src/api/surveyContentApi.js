import { apiClient } from './apiClient.js';

/** Sustituye el contenido interno de una encuesta. */
export function replaceSurveyContent(surveyId, contentData) {
  return apiClient(`/surveys/${surveyId}/content`, {
    method: 'PUT',
    body: contentData,
  });
}
