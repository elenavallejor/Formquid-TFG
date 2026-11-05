import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError, apiClient, resolveProfilePhotoUrl, resolveSurveyImageUrl, setUnauthorizedHandler } from './apiClient.js';

function jsonResponse(data, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: () => 'application/json' },
    json: async () => data,
  };
}

describe('apiClient', () => {
  it('resuelve la portada subida respecto a la URL del backend', () => {
    expect(resolveSurveyImageUrl('/api/v1/public/surveys/12/cover/imagen.png'))
      .toBe('http://localhost:8080/api/v1/public/surveys/12/cover/imagen.png');
    expect(resolveSurveyImageUrl('https://example.test/imagen.png')).toBe('https://example.test/imagen.png');
  });

  it('resuelve la foto de perfil subida respecto a la URL del backend', () => {
    expect(resolveProfilePhotoUrl('/api/v1/users/7/photo/imagen.png'))
      .toBe('http://localhost:8080/api/v1/users/7/photo/imagen.png');
  });

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

  it.each([{ status: 503, ok: false }, { status: 200, ok: true }])('rechaza inicializaciones CSRF sin token: %j', async (response) => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0';
    fetch.mockResolvedValue(response);
    await expect(apiClient('/csrf-test', { method: 'POST' })).rejects.toMatchObject({ status: response.status, name: 'ApiError' });
  });
  it('comparte la inicialización CSRF entre modificaciones simultáneas', async () => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0';
    let resolve;
    fetch.mockImplementationOnce(() => new Promise(done => { resolve = done; })).mockResolvedValue(jsonResponse({ ok: true }));
    const first = apiClient('/first', { method: 'POST', body: 'text' });
    const second = apiClient('/second', { method: 'POST', body: null });
    expect(fetch).toHaveBeenCalledTimes(1);
    document.cookie = 'XSRF-TOKEN=nuevo';
    resolve({ ok: true, status: 200 });
    await expect(Promise.all([first, second])).resolves.toHaveLength(2);
  });
  it.each([
    [{ headers: { get: () => null }, text: async () => '' }, null],
    [{ headers: { get: () => 'application/json' }, json: async () => { throw new Error('invalid'); } }, null],
  ])('tolera respuestas vacías o JSON inválido', async (response, expected) => {
    fetch.mockResolvedValue({ ok: true, status: 200, ...response });
    await expect(apiClient('/optional')).resolves.toBe(expected);
  });
  it('usa errores de texto y el campo error y admite cancelar GET', async () => {
    fetch.mockResolvedValueOnce({ ok: false, status: 500, headers: { get: () => 'text/plain' }, text: async () => 'Error de texto' });
    await expect(apiClient('/error-text')).rejects.toThrow('Error de texto');
    fetch.mockResolvedValueOnce(jsonResponse({ error: 'Error alternativo' }, 400));
    await expect(apiClient('/error-alternative')).rejects.toThrow('Error alternativo');
    fetch.mockResolvedValueOnce(jsonResponse({ ok: true }));
    const controller = new AbortController();
    await expect(apiClient('/abortable', { signal: controller.signal, headers: { Z: 'z', A: 'a' } })).resolves.toEqual({ ok: true });
    expect(resolveProfilePhotoUrl(null)).toBeNull();
    expect(resolveSurveyImageUrl(undefined)).toBeUndefined();
  });
});
