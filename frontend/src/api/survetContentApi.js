import { apiClient } from './apiClient.js';

export async function addTextToSurvey(surveyId, textData) {
  return apiClient(`/surveys/${surveyId}/content/texts`, {
    method: 'POST',
    body: textData,
  });
}

export async function addLocalQuestion(surveyId, questionData) {
  return apiClient(`/surveys/${surveyId}/content/questions/local`, {
    method: 'POST',
    body: questionData,
  });
}

export async function addFixedQuestion(surveyId, questionId) {
  return apiClient(`/surveys/${surveyId}/content/questions/fixed`, {
    method: 'POST',
    body: { questionId },
  });
}

export async function addBlockToSurvey(surveyId, blockId) {
  return apiClient(`/surveys/${surveyId}/content/blocks`, {
    method: 'POST',
    body: { blockId },
  });
}

export async function updateSurveyText(surveyId, itemId, textData) {
  return apiClient(`/surveys/${surveyId}/content/texts/${itemId}`, {
    method: 'PUT',
    body: textData,
  });
}

export async function updateSurveyQuestion(surveyId, itemId, questionData) {
  return apiClient(`/surveys/${surveyId}/content/questions/${itemId}`, {
    method: 'PUT',
    body: questionData,
  });
}

export async function updateQuestionRequired(surveyId, itemId, required) {
  return apiClient(`/surveys/${surveyId}/content/questions/${itemId}/required`, {
    method: 'PATCH',
    body: { required },
  });
}

export async function reorderSurveyContent(surveyId, itemsOrder) {
  return apiClient(`/surveys/${surveyId}/content/reorder`, {
    method: 'PATCH',
    body: { itemsOrder },
  });
}

export async function deleteSurveyContentItem(surveyId, itemId) {
  return apiClient(`/surveys/${surveyId}/content/${itemId}`, {
    method: 'DELETE',
  });
}