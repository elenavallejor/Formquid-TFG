const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
).replace(/\/$/, '');

export async function apiClient(path, options = {}) {
  const { body, headers, ...customOptions } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
    ...customOptions,
  });

  const contentType = response.headers.get('content-type');
  const hasJsonContent = contentType?.includes('application/json');

  const data = hasJsonContent
    ? await response.json().catch(() => null)
    : null;

  if (response.status === 401) {
    throw new Error('La sesión ha expirado. Inicia sesión de nuevo.');
  }

  if (!response.ok) {
    throw new Error(
      data?.message || 'Error de comunicación con el servidor.'
    );
  }

  return data;
}