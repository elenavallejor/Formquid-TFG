import { apiClient } from './apiClient.js';

export async function getCollaborators(surveyId) {
  return apiClient(`/surveys/${surveyId}/collaborators`, { method: 'GET' });
}

export async function getCollaborationRequests(surveyId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests`, { method: 'GET' });
}

export async function createCollaborationRequest(surveyId, targetData) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests`, {
    method: 'POST',
    body: targetData,
  });
}

export async function acceptCollaborationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests/${requestId}/accept`, {
    method: 'POST',
  });
}

export async function rejectCollaborationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/collaboration-requests/${requestId}`, {
    method: 'DELETE',
  });
}

export async function removeCollaborator(surveyId, collaboratorId) {
  return apiClient(`/surveys/${surveyId}/collaborators/${collaboratorId}`, {
    method: 'DELETE',
  });
}

export async function requestPublication(surveyId) {
  return apiClient(`/surveys/${surveyId}/publication-requests`, {
    method: 'POST',
  });
}

export async function acceptPublicationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/publication-requests/${requestId}/accept`, {
    method: 'POST',
  });
}

export async function rejectPublicationRequest(surveyId, requestId) {
  return apiClient(`/surveys/${surveyId}/publication-requests/${requestId}`, {
    method: 'DELETE',
  });
}