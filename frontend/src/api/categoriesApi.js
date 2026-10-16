import { apiClient } from './apiClient.js';

export function getCategories({ surveyOnly = false } = {}) {
  return apiClient(`/categories?surveyOnly=${surveyOnly}`);
}

export function createCategory(name) {
  return apiClient('/categories', { method: 'POST', body: { name } });
}
