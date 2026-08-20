import { apiClient } from '../../../lib/apiClient';

export function createRegistrationRequest(payload) {
  return apiClient('/registration-requests', {
    method: 'POST',
    body: payload,
  });
}