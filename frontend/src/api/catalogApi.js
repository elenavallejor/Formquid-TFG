import { apiClient } from './apiClient.js';

/**
 * Centraliza las operaciones disponibles sobre los elementos del catálogo.
 */
function buildPaginatedPath(path, { page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();

  params.set('page', String(page));
  params.set('size', String(size));

  return `${path}?${params.toString()}`;
}

export function getCatalogQuestions(options = {}) {
  const { signal, page = 0, size = 10 } = options;

  return apiClient(
    buildPaginatedPath('/catalog/questions', { page, size }),
    {
      method: 'GET',
      signal,
    },
  );
}

export function getCatalogQuestionById(questionId) {
  return apiClient(`/catalog/questions/${questionId}`, {
    method: 'GET',
  });
}

export function createCatalogQuestion(data) {
  return apiClient('/catalog/questions', {
    method: 'POST',
    body: data,
  });
}

export function deleteCatalogQuestion(questionId) {
  return apiClient(`/catalog/questions/${questionId}`, {
    method: 'DELETE',
  });
}

export function getCatalogBlocks(options = {}) {
  const { signal, page = 0, size = 10 } = options;

  return apiClient(
    buildPaginatedPath('/catalog/blocks', { page, size }),
    {
      method: 'GET',
      signal,
    },
  );
}

export function getCatalogBlockById(blockId) {
  return apiClient(`/catalog/blocks/${blockId}`, {
    method: 'GET',
  });
}

export function createCatalogBlock(data) {
  return apiClient('/catalog/blocks', {
    method: 'POST',
    body: data,
  });
}

export function deleteCatalogBlock(blockId) {
  return apiClient(`/catalog/blocks/${blockId}`, {
    method: 'DELETE',
  });
}

export function getCatalogTemplates(options = {}) {
  const { signal, page = 0, size = 10 } = options;

  return apiClient(
    buildPaginatedPath('/catalog/templates', { page, size }),
    {
      method: 'GET',
      signal,
    },
  );
}

export function getCatalogTemplateById(templateId) {
  return apiClient(`/catalog/templates/${templateId}`, {
    method: 'GET',
  });
}

export function createCatalogTemplate(data) {
  return apiClient('/catalog/templates', {
    method: 'POST',
    body: data,
  });
}

export function deleteCatalogTemplate(templateId) {
  return apiClient(`/catalog/templates/${templateId}`, {
    method: 'DELETE',
  });
}