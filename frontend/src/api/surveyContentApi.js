import { apiClient } from './apiClient.js';

/**
 * Gestiona la creación, edición, reordenación y eliminación del contenido interno de una encuesta.
 */
export function addTextToSurvey(surveyId, textData) {
  return apiClient(`/surveys/${surveyId}/content/texts`, {
    method: 'POST',
    body: textData,
  });
}

export function addLocalQuestion(surveyId, questionData) {
  return apiClient(`/surveys/${surveyId}/content/questions/local`, {
    method: 'POST',
    body: questionData,
  });
}

export function addFixedQuestion(surveyId, questionId) {
  return apiClient(`/surveys/${surveyId}/content/questions/fixed`, {
    method: 'POST',
    body: { questionId },
  });
}

export function addBlockToSurvey(surveyId, blockId) {
  return apiClient(`/surveys/${surveyId}/content/blocks`, {
    method: 'POST',
    body: { blockId },
  });
}

export function updateSurveyText(surveyId, itemId, textData) {
  return apiClient(`/surveys/${surveyId}/content/texts/${itemId}`, {
    method: 'PUT',
    body: textData,
  });
}

export function updateSurveyQuestion(surveyId, itemId, questionData) {
  return apiClient(`/surveys/${surveyId}/content/questions/${itemId}`, {
    method: 'PUT',
    body: questionData,
  });
}

export function updateQuestionRequired(surveyId, itemId, required) {
  return apiClient(`/surveys/${surveyId}/content/questions/${itemId}/required`, {
    method: 'PATCH',
    body: { required },
  });
}

export function reorderSurveyContent(surveyId, itemsOrder) {
  return apiClient(`/surveys/${surveyId}/content/reorder`, {
    method: 'PATCH',
    body: { itemsOrder },
  });
}

export function deleteSurveyContentItem(surveyId, itemId) {
  return apiClient(`/surveys/${surveyId}/content/${itemId}`, {
    method: 'DELETE',
  });
}
