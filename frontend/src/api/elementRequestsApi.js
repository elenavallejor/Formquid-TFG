import { apiClient } from './apiClient.js';

/**
 * Gestiona la creación y resolución administrativa de solicitudes de elementos del catálogo.
 */
export function createElementRequest(data) {
  return apiClient('/element-requests', {
    method: 'POST',
    body: data,
  });
}

export function getPendingElementRequests() {
  return apiClient('/admin/element-requests', { method: 'GET' });
}

export function approveElementRequest(requestId) {
  return apiClient(`/admin/element-requests/${requestId}/approve`, {
    method: 'POST',
  });
}

export function rejectElementRequest(requestId) {
  return apiClient(`/admin/element-requests/${requestId}`, {
    method: 'DELETE',
  });
}
