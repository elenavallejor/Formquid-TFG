import { apiClient } from '../../../lib/apiClient';

export function login(credentials) {
  return apiClient('/auth/login', {
    method: 'POST',
    body: credentials,
  });
}