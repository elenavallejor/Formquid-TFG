import { apiClient } from '../../../lib/apiClient';

export function getCurrentUser() {
  return apiClient('/users/me', {
    method: 'GET',
  });
}