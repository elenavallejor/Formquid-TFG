import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError, apiClient, setUnauthorizedHandler } from './apiClient.js';

function jsonResponse(data, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: () => 'application/json' },
    json: async () => data,
  };
}

describe('apiClient', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    setUnauthorizedHandler(null);
    vi.unstubAllGlobals();
    document.cookie = 'XSRF-TOKEN=; Max-Age=0';
  });

  it('envía credenciales y procesa respuestas JSON', async () => {
    fetch.mockResolvedValue(jsonResponse({ id: 7 }));
    await expect(apiClient('/users/me')).resolves.toEqual({ id: 7 });
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/v1/users/me', expect.objectContaining({
      method: 'GET',
      credentials: 'include',
    }));
  });

  it('comparte una petición GET simultánea y vuelve a solicitarla después', async () => {
    let resolveRequest;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveRequest = resolve; }));
    const first = apiClient('/catalog/one');
    const second = apiClient('/catalog/one');
    expect(first).toBe(second);
    expect(fetch).toHaveBeenCalledTimes(1);
    resolveRequest(jsonResponse({ id: 1 }));
    await expect(first).resolves.toEqual({ id: 1 });
    fetch.mockResolvedValueOnce(jsonResponse({ id: 2 }));
    await expect(apiClient('/catalog/one')).resolves.toEqual({ id: 2 });
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('envía JSON con CSRF al modificar datos', async () => {
    fetch.mockResolvedValue(jsonResponse({ saved: true }));
    await apiClient('/surveys', { method: 'POST', body: { title: 'Encuesta' } });
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/api/v1/surveys', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ title: 'Encuesta' }),
      headers: expect.objectContaining({ 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'token-de-prueba' }),
    }));
  });

  it('no fuerza Content-Type para FormData', async () => {
    fetch.mockResolvedValue(jsonResponse({ saved: true }));
    const body = new FormData();
    body.append('file', 'contenido');
    await apiClient('/import', { method: 'POST', body });
    const options = fetch.mock.calls[0][1];
    expect(options.body).toBe(body);
    expect(options.headers).not.toHaveProperty('Content-Type');
  });

  it('devuelve null para 204 y el texto de respuestas no JSON', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 204, headers: { get: () => null } });
    await expect(apiClient('/empty')).resolves.toBeNull();
    fetch.mockResolvedValueOnce({ ok: true, status: 200, headers: { get: () => 'text/plain' }, text: async () => 'listo' });
    await expect(apiClient('/text')).resolves.toBe('listo');
  });

  it('notifica el 401 y conserva el mensaje de error del servidor', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetch.mockResolvedValue(jsonResponse({ message: 'Sesión caducada' }, 401));
    await expect(apiClient('/private')).rejects.toMatchObject({
      name: 'ApiError',
      status: 401,
      message: 'Sesión caducada',
    });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('usa un error legible cuando el servidor no envía mensaje', async () => {
    fetch.mockResolvedValue(jsonResponse({}, 500));
    await expect(apiClient('/broken')).rejects.toEqual(new ApiError('No se ha podido completar la solicitud.', 500, {}));
  });
});
