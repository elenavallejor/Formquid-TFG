import { apiClient } from './apiClient.js';

export function getCategories({ surveyOnly = false } = {}) {
  return apiClient(`/categories?surveyOnly=${surveyOnly}`);
}

export function createCategory(name, icon) {
  return apiClient('/categories', { method: 'POST', body: { name, icon } });
}

export function updateCategoryIcon(code, icon) {
  return apiClient(`/categories/${encodeURIComponent(code)}/icon`, {
    method: 'PATCH',
    body: { icon },
  });
}
