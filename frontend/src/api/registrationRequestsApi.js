import { apiClient } from './apiClient.js';

/**
 * Gestiona la creación y resolución administrativa de solicitudes de registro.
 */
export function createRegistrationRequest(data) {
  return apiClient('/registration-requests', {
    method: 'POST',
    body: data,
  });
}

export function getPendingRegistrationRequests() {
  return apiClient('/admin/registration-requests', {
    method: 'GET',
  });
}

export function approveRegistrationRequest(requestId) {
  return apiClient(`/admin/registration-requests/${requestId}/approve`, {
    method: 'POST',
  });
}

export function rejectRegistrationRequest(requestId) {
  return apiClient(`/admin/registration-requests/${requestId}`, {
    method: 'DELETE',
  });
}
