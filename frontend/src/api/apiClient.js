/**
 * Centraliza las peticiones HTTP de FormQuid, evita GET simultáneos duplicados y gestiona credenciales, CSRF, serialización y errores.
 */
const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1')
).replace(/\/$/, '');

export function resolveSurveyImageUrl(imageKey) {
  if (!imageKey?.startsWith('/api/v1/public/surveys/')) return imageKey;
  return new URL(`${API_BASE_URL}${imageKey.slice('/api/v1'.length)}`, window.location.origin).href;
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);
const pendingGetRequests = new Map();

let csrfRequestPromise = null;
let unauthorizedHandler = null;

export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === 'function' ? handler : null;
}

function getCookie(name) {
  const prefix = `${name}=`;
  const cookie = document.cookie
    .split('; ')
    .find((row) => row.startsWith(prefix));

  return cookie
    ? decodeURIComponent(cookie.substring(prefix.length))
    : null;
}

async function ensureCsrfToken() {
  const existingToken = getCookie('XSRF-TOKEN');

  if (existingToken) {
    return existingToken;
  }

  if (!csrfRequestPromise) {
    csrfRequestPromise = fetch(`${API_BASE_URL}/auth/csrf`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
      },
    }).finally(() => {
      csrfRequestPromise = null;
    });
  }

  const response = await csrfRequestPromise;

  if (!response.ok) {
    throw new ApiError(
      'No se ha podido inicializar la protección CSRF.',
      response.status,
    );
  }

  const csrfToken = getCookie('XSRF-TOKEN');

  if (!csrfToken) {
    throw new ApiError(
      'El servidor no ha proporcionado un token CSRF.',
      response.status,
    );
  }

  return csrfToken;
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json().catch(() => null);
  }

  const text = await response.text();
  return text || null;
}

function getErrorMessage(data) {
  if (typeof data === 'string' && data.trim()) {
    return data;
  }

  return (
    data?.message ||
    data?.error ||
    'No se ha podido completar la solicitud.'
  );
}

function getRequestKey(path, headers) {
  const normalizedHeaders = Object.entries(headers)
    .sort(([firstKey], [secondKey]) => firstKey.localeCompare(secondKey));

  return `${path}:${JSON.stringify(normalizedHeaders)}`;
}

async function executeRequest(path, body, headers, customOptions, method) {
  const csrfToken = SAFE_METHODS.has(method)
    ? null
    : await ensureCsrfToken();

  const isFormData =
    typeof FormData !== 'undefined' && body instanceof FormData;

  const hasBody = body !== undefined && body !== null;

  let requestBody;

  if (!hasBody) {
    requestBody = undefined;
  } else if (isFormData || typeof body === 'string') {
    requestBody = body;
  } else {
    requestBody = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...customOptions,
    method,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(hasBody && !isFormData
        ? { 'Content-Type': 'application/json' }
        : {}),
      ...(csrfToken
        ? { 'X-XSRF-TOKEN': csrfToken }
        : {}),
      ...headers,
    },
    body: requestBody,
  });

  const data = await parseResponse(response);

  if (!response.ok) {
    if (response.status === 401) {
      unauthorizedHandler?.();
    }

    throw new ApiError(
      getErrorMessage(data),
      response.status,
      data,
    );
  }

  return data;
}

export function apiClient(path, options = {}) {
  const {
    body,
    headers = {},
    ...customOptions
  } = options;

  const method = (customOptions.method ?? 'GET').toUpperCase();
  const request = () => executeRequest(
    path,
    body,
    headers,
    customOptions,
    method,
  );

  if (method !== 'GET' || customOptions.signal) {
    return request();
  }

  const requestKey = getRequestKey(path, headers);
  const pendingRequest = pendingGetRequests.get(requestKey);

  if (pendingRequest) {
    return pendingRequest;
  }

  const requestPromise = request().finally(() => {
    pendingGetRequests.delete(requestKey);
  });

  pendingGetRequests.set(requestKey, requestPromise);
  return requestPromise;
}
