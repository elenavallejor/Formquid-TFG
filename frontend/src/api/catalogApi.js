import { apiClient } from './apiClient.js';

const CACHE_DURATION = 30_000;
const pageCaches = {
  questions: new Map(),
  blocks: new Map(),
  templates: new Map(),
};

function buildPaginatedPath(path, { page = 0, size = 10, mine } = {}) {
  const params = new URLSearchParams();

  params.set('page', String(page));
  params.set('size', String(size));
  if (typeof mine === 'boolean') params.set('mine', String(mine));

  return `${path}?${params.toString()}`;
}

function withAbortSignal(promise, signal) {
  if (!signal) return promise;

  if (signal.aborted) {
    return Promise.reject(
      new DOMException('La petición se ha cancelado.', 'AbortError'),
    );
  }

  return new Promise((resolve, reject) => {
    const handleAbort = () => {
      reject(
        new DOMException('La petición se ha cancelado.', 'AbortError'),
      );
    };

    signal.addEventListener('abort', handleAbort, { once: true });

    promise.then(
      (value) => {
        signal.removeEventListener('abort', handleAbort);
        resolve(value);
      },
      (error) => {
        signal.removeEventListener('abort', handleAbort);
        reject(error);
      },
    );
  });
}

function getCachedPage(cache, path, signal, refresh = false) {
  const now = Date.now();
  const cached = cache.get(path);

  if (!refresh && cached && cached.expiresAt > now) {
    return withAbortSignal(cached.promise, signal);
  }

  const promise = apiClient(path, { method: 'GET' }).catch((error) => {
    if (cache.get(path)?.promise === promise) {
      cache.delete(path);
    }

    throw error;
  });

  cache.set(path, {
    promise,
    expiresAt: now + CACHE_DURATION,
  });

  return withAbortSignal(promise, signal);
}

function clearCache(cache) {
  cache.clear();
}

export function getCatalogQuestions(options = {}) {
  const {
    signal,
    page = 0,
    size = 10,
    mine = false,
    refresh = false,
  } = options;

  const path = buildPaginatedPath('/catalog/questions', {
    page,
    size,
    mine,
  });

  return getCachedPage(
    pageCaches.questions,
    path,
    signal,
    refresh,
  );
}

export function getCatalogQuestionsForBuilder(options = {}) {
  const {
    signal,
    page = 0,
    size = 10,
    refresh = false,
  } = options;

  const path = buildPaginatedPath('/catalog/questions/builder', {
    page,
    size,
  });

  return getCachedPage(
    pageCaches.questions,
    path,
    signal,
    refresh,
  );
}

export function getCatalogQuestionById(questionId) {
  return apiClient(`/catalog/questions/${questionId}`, {
    method: 'GET',
  });
}

export async function createCatalogQuestion(data) {
  const response = await apiClient('/catalog/questions', {
    method: 'POST',
    body: data,
  });

  clearCache(pageCaches.questions);

  return response;
}

export async function updateCatalogQuestion(questionId, data) {
  const response = await apiClient(`/catalog/questions/${questionId}`, {
    method: 'PUT',
    body: data,
  });
  clearCache(pageCaches.questions);
  return response;
}

export async function deleteCatalogQuestion(questionId) {
  const response = await apiClient(
    `/catalog/questions/${questionId}`,
    {
      method: 'DELETE',
    },
  );

  clearCache(pageCaches.questions);

  return response;
}

export function getCatalogBlocks(options = {}) {
  const {
    signal,
    page = 0,
    size = 10,
    mine = false,
    refresh = false,
  } = options;

  const path = buildPaginatedPath('/catalog/blocks', {
    page,
    size,
    mine,
  });

  return getCachedPage(
    pageCaches.blocks,
    path,
    signal,
    refresh,
  );
}

export function getCatalogBlocksForBuilder(options = {}) {
  const {
    signal,
    page = 0,
    size = 10,
    refresh = false,
  } = options;

  const path = buildPaginatedPath('/catalog/blocks/builder', {
    page,
    size,
  });

  return getCachedPage(
    pageCaches.blocks,
    path,
    signal,
    refresh,
  );
}

export function getCatalogBlockById(blockId) {
  return apiClient(`/catalog/blocks/${blockId}`, {
    method: 'GET',
  });
}

export async function createCatalogBlock(data) {
  const response = await apiClient('/catalog/blocks', {
    method: 'POST',
    body: data,
  });

  clearCache(pageCaches.blocks);

  return response;
}

export async function updateCatalogBlock(blockId, data) {
  const response = await apiClient(`/catalog/blocks/${blockId}`, {
    method: 'PUT',
    body: data,
  });
  clearCache(pageCaches.blocks);
  return response;
}

export async function deleteCatalogBlock(blockId) {
  const response = await apiClient(
    `/catalog/blocks/${blockId}`,
    {
      method: 'DELETE',
    },
  );

  clearCache(pageCaches.blocks);

  return response;
}

export function getCatalogTemplates(options = {}) {
  const {
    signal,
    page = 0,
    size = 10,
    mine = false,
    refresh = false,
  } = options;

  const path = buildPaginatedPath('/catalog/templates', {
    page,
    size,
    mine,
  });

  return getCachedPage(
    pageCaches.templates,
    path,
    signal,
    refresh,
  );
}

export function getCatalogTemplateById(templateId) {
  return apiClient(`/catalog/templates/${templateId}`, {
    method: 'GET',
  });
}

export async function createCatalogTemplate(data) {
  const response = await apiClient('/catalog/templates', {
    method: 'POST',
    body: data,
  });

  clearCache(pageCaches.templates);

  return response;
}

export async function updateCatalogTemplate(templateId, data) {
  const response = await apiClient(`/catalog/templates/${templateId}`, {
    method: 'PUT',
    body: data,
  });
  clearCache(pageCaches.templates);
  return response;
}

export async function deleteCatalogTemplate(templateId) {
  const response = await apiClient(
    `/catalog/templates/${templateId}`,
    {
      method: 'DELETE',
    },
  );

  clearCache(pageCaches.templates);

  return response;
}
