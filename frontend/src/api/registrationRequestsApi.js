import { apiClient } from './apiClient.js';

export async function createRegistrationRequest(data) {
  return apiClient('/registration-requests', {
    method: 'POST',
    body: data,
  });
}

export async function getPendingRegistrationRequests() {
  return apiClient('/admin/registration-requests', { method: 'GET' });
}

export async function approveRegistrationRequest(requestId) {
  return apiClient(`/admin/registration-requests/${requestId}/approve`, {
    method: 'POST',
  });
}

export async function rejectRegistrationRequest(requestId) {
  return apiClient(`/admin/registration-requests/${requestId}`, {
    method: 'DELETE',
  });
}