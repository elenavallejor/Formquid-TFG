import { apiClient } from './apiClient';

export function login(credentials) {
  return apiClient('/auth/login', {
    method: 'POST',
    body: credentials,
  });
}

export function logout() {
  return apiClient('/auth/logout', {
    method: 'POST',
  });
}

export function getCurrentUser() {
  return apiClient('/users/me');
}

export function createRegistrationRequest(payload) {
  return apiClient('/registration-requests', {
    method: 'POST',
    body: payload,
  });
}