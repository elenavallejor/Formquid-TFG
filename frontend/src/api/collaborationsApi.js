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

export function getCollaborationOverview() {
  return apiClient('/collaboration-requests', { method: 'GET' });
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

export function updateCollaboratorPermission(surveyId, collaboratorId, permission) {
  return apiClient(`/surveys/${surveyId}/collaborators/${collaboratorId}`, {
    method: 'PATCH',
    body: { permission },
  });
}

export function requestPublication(surveyId, message = '') {
  return apiClient(`/surveys/${surveyId}/publication-requests`, {
    method: 'POST',
    body: { message: message.trim() || null },
  });
}

export function getPublicationRequests(surveyId) {
  return apiClient(`/surveys/${surveyId}/publication-requests`, {
    method: 'GET',
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
