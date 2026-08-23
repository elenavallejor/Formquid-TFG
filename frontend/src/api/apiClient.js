const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080/api/v1'
).replace(/\/$/, '');

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS','TRACE',]);

let csrfRequestPromise = null;

/**
 * Error HTTP normalizado de la API.
 */
export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);

    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Obtiene el valor de una cookie accesible desde JavaScript.
 */
function getCookie(name) {
  const prefix = `${name}=`;

  const cookie = document.cookie
    .split('; ')
    .find((row) => row.startsWith(prefix));

  if (!cookie) {
    return null;
  }

  return decodeURIComponent(
    cookie.substring(prefix.length)
  );
}

/**
 * Obtiene un token CSRF únicamente cuando todavía no existe.
 */
async function ensureCsrfToken() {
  const existingToken = getCookie('XSRF-TOKEN');

  if (existingToken) {
    return existingToken;
  }

  if (!csrfRequestPromise) {
    csrfRequestPromise = fetch(
      `${API_BASE_URL}/auth/csrf`,
      {
        method: 'GET',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
        },
      }
    ).finally(() => {
      csrfRequestPromise = null;
    });
  }

  const response = await csrfRequestPromise;

  if (!response.ok) {
    throw new ApiError(
      'No se ha podido inicializar la protección CSRF.',
      response.status
    );
  }

  const csrfToken = getCookie('XSRF-TOKEN');

  if (!csrfToken) {
    throw new ApiError(
      'El servidor no ha proporcionado un token CSRF.',
      response.status
    );
  }

  return csrfToken;
}

/**
 * Intenta interpretar el cuerpo de una respuesta.
 */
async function parseResponse(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType =
    response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json().catch(() => null);
  }

  const text = await response.text();

  return text || null;
}

/**
 * Cliente HTTP central de FormQuid.
 */
export async function apiClient(path, options = {}) {
  const {
    body,
    headers = {},
    ...customOptions
  } = options;

  const method = (
    customOptions.method ?? 'GET'
  ).toUpperCase();

  const requiresCsrf = !SAFE_METHODS.has(method);

  const csrfToken = requiresCsrf
    ? await ensureCsrfToken()
    : null;

  const isFormData =
    typeof FormData !== 'undefined' &&
    body instanceof FormData;

  const hasBody =
    body !== undefined &&
    body !== null;

  let requestBody;

  if (!hasBody) {
    requestBody = undefined;
  } else if (
    isFormData ||
    typeof body === 'string'
  ) {
    requestBody = body;
  } else {
    requestBody = JSON.stringify(body);
  }

  const response = await fetch(
    `${API_BASE_URL}${path}`,
    {
      ...customOptions,

      method,

      credentials: 'include',

      headers: {
        Accept: 'application/json',

        ...(hasBody && !isFormData
          ? {
              'Content-Type':
                'application/json',
            }
          : {}),

        ...(csrfToken
          ? {
              'X-XSRF-TOKEN':
                csrfToken,
            }
          : {}),

        ...headers,
      },

      body: requestBody,
    }
  );

  const data = await parseResponse(response);

  if (!response.ok) {
    const message =
      data?.message ||
      data?.error ||
      'No se ha podido completar la solicitud.';

    throw new ApiError(
      message,
      response.status,
      data
    );
  }

  return data;
}