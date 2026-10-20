import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from './apiClient.js';
import {
  accessPasswordProtectedSurvey,
  createSurveyFromImport,
  getAdminSurveys,
  getMySurveys,
  getPublicSurveys,
  getResultSurveys,
  getSurveyDirectory,
  previewSurveyImport,
  updateSurveyLifecycle,
} from './surveysApi.js';

vi.mock('./apiClient.js', () => ({ apiClient: vi.fn() }));

function lastRequest() {
  const [url, options] = apiClient.mock.lastCall;
  return { url: new URL(url, 'https://example.test'), options };
}

describe('surveysApi', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    apiClient.mockResolvedValue({});
  });

  it('normaliza búsqueda y filtros de las encuestas públicas', () => {
    getPublicSurveys({ search: '  ciencia  ', tag: ' salud ', accessMode: 'PUBLIC', page: 2 });
    const { url, options } = lastRequest();
    expect(url.pathname).toBe('/public/surveys/page');
    expect(Object.fromEntries(url.searchParams)).toEqual({ search: 'ciencia', tag: 'salud', accessMode: 'PUBLIC', page: '2', size: '9' });
    expect(options).toEqual({ method: 'GET' });
  });

  it('ordena las encuestas propias y transmite la señal de cancelación', () => {
    const signal = new AbortController().signal;
    getMySurveys({ search: '  clima ', lifecycleStatus: 'OPEN', sortOrder: 'OLDEST', signal });
    const { url, options } = lastRequest();
    expect(url.searchParams.get('search')).toBe('clima');
    expect(url.searchParams.get('lifecycleStatus')).toBe('OPEN');
    expect(url.searchParams.get('sort')).toBe('updatedAt,asc');
    expect(options.signal).toBe(signal);
  });

  it('envía los filtros de resultados y conserva el orden solicitado', () => {
    getResultSurveys({ searchFields: ['TITLE', 'OWNER'], lifecycleStatus: 'CLOSED', sortOrder: 'OLDEST', page: 1, size: 18 });
    const { url } = lastRequest();
    expect(url.pathname).toBe('/surveys/results');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      searchFields: 'TITLE,OWNER', lifecycleStatus: 'CLOSED', sortOrder: 'OLDEST', page: '1', size: '18',
    });
  });

  it('omite filtros no seleccionados en el directorio y la supervisión', () => {
    getSurveyDirectory();
    expect(lastRequest().url.searchParams.has('lifecycleStatus')).toBe(false);
    getAdminSurveys({ visibility: 'UNLISTED', lifecycleStatus: 'DRAFT' });
    const { url } = lastRequest();
    expect(url.pathname).toBe('/admin/surveys');
    expect(url.searchParams.get('visibility')).toBe('UNLISTED');
    expect(url.searchParams.get('lifecycleStatus')).toBe('DRAFT');
  });

  it('sube el archivo ODT como formulario multipart', () => {
    const file = new File(['datos'], 'encuesta.odt');
    previewSurveyImport(file);
    const { url, options } = lastRequest();
    expect(url.pathname).toBe('/surveys/import/preview');
    expect(options.method).toBe('POST');
    expect(options.body).toBeInstanceOf(FormData);
    expect(options.body.get('file')).toBe(file);
  });

  it('conserva la estructura de encuesta importada y las credenciales de acceso', () => {
    const survey = { title: 'Estudio' };
    const content = [{ itemType: 'QUESTION' }];
    createSurveyFromImport(survey, content);
    expect(apiClient).toHaveBeenLastCalledWith('/surveys/from-import', { method: 'POST', body: { survey, content } });
    accessPasswordProtectedSurvey('ABC', 'clave');
    expect(apiClient).toHaveBeenLastCalledWith('/public/surveys/ABC/access', {
      method: 'POST', body: { accessPassword: 'clave' },
    });
    updateSurveyLifecycle(12, 'CLOSED');
    expect(apiClient).toHaveBeenLastCalledWith('/surveys/12/lifecycle', {
      method: 'PATCH', body: { lifecycleStatus: 'CLOSED' },
    });
  });
});
