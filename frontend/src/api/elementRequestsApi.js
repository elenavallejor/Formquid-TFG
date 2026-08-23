import { apiClient } from './apiClient.js';

export async function createElementRequest(data) {
  return apiClient('/element-requests', {
    method: 'POST',
    body: data,
  });
}

export async function getPendingElementRequests() {
  return apiClient('/admin/element-requests', { method: 'GET' });
}

export async function approveElementRequest(requestId) {
  return apiClient(`/admin/element-requests/${requestId}/approve`, {
    method: 'POST',
  });
}

export async function rejectElementRequest(requestId) {
  return apiClient(`/admin/element-requests/${requestId}`, {
    method: 'DELETE',
  });
}