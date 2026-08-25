import { apiClient } from './apiClient.js';

/**
 * Gestiona colaboradores y solicitudes de colaboración o publicación asociadas a una encuesta.
 */
export function getCollaborators(surveyId) {
  return apiClient(`/surveys/${surveyId}/collaborators`, {
    method: 'GET',
  });
}

export function getCollaborationRequests(surveyId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests`, {
    method: 'GET',
  });
}

export function createCollaborationRequest(surveyId, targetData) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests`, {
    method: 'POST',
    body: targetData,
  });
}

export function acceptCollaborationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests/${requestId}/accept`, {
    method: 'POST',
  });
}

export function rejectCollaborationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests/${requestId}`, {
    method: 'DELETE',
  });
}

export function removeCollaborator(surveyId, collaboratorId) {
  return apiClient(`/surveys/${surveyId}/collaborators/${collaboratorId}`, {
    method: 'DELETE',
  });
}

export function requestPublication(surveyId) {
  return apiClient(`/surveys/${surveyId}/publication-requests`, {
    method: 'POST',
  });
}

export function acceptPublicationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/publication-requests/${requestId}/accept`, {
    method: 'POST',
  });
}

export function rejectPublicationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/publication-requests/${requestId}`, {
    method: 'DELETE',
  });
}
