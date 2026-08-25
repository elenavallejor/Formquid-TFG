import { apiClient } from './apiClient.js';

function buildCatalogListPath(path, {
  search = '',
  category = 'ALL',
  sortOrder = 'NEWEST',
  page = 0,
  size = 10,
} = {}) {
  const params = new URLSearchParams();
  const normalizedSearch = search.trim();

  if (normalizedSearch) {
    params.set('search', normalizedSearch);
  }

  if (category && category !== 'ALL') {
    params.set('category', category);
  }

  params.set(
    'sort',
    `updatedAt,${sortOrder === 'OLDEST' ? 'asc' : 'desc'}`,
  );
  params.set('page', String(page));
  params.set('size', String(size));

  return `${path}?${params.toString()}`;
}

export function getCatalogQuestions(options = {}) {
  const { signal, ...query } = options;
  return apiClient(buildCatalogListPath('/catalog/questions', query), {
    method: 'GET',
    signal,
  });
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
  const { signal, ...query } = options;
  return apiClient(buildCatalogListPath('/catalog/blocks', query), {
    method: 'GET',
    signal,
  });
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
  const { signal, ...query } = options;
  return apiClient(buildCatalogListPath('/catalog/templates', query), {
    method: 'GET',
    signal,
  });
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