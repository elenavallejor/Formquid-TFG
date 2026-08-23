import { apiClient } from './apiClient.js';

// Preguntas
export async function getCatalogQuestions() {
  return apiClient('/catalog/questions', { method: 'GET' });
}

export async function createCatalogQuestion(data) {
  return apiClient('/catalog/questions', {
    method: 'POST',
    body: data,
  });
}

// Bloques
export async function getCatalogBlocks() {
  return apiClient('/catalog/blocks', { method: 'GET' });
}

export async function createCatalogBlock(data) {
  return apiClient('/catalog/blocks', {
    method: 'POST',
    body: data,
  });
}

// Plantillas
export async function getCatalogTemplates() {
  return apiClient('/catalog/templates', { method: 'GET' });
}

export async function createCatalogTemplate(data) {
  return apiClient('/catalog/templates', {
    method: 'POST',
    body: data,
  });
}