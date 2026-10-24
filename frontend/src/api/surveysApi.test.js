import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from './apiClient.js';
import {
  accessPasswordProtectedSurvey,
  createSurveyFromImport,
  deleteSurvey,
  getAdminSurveys,
  getMySurveys,
  getPublicSurveys,
  getResultSurveys,
  getSurveyDirectory,
  previewSurveyImport,
  updateSurveyLifecycle,
  uploadSurveyCover,
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
    getPublicSurveys({ search: '  ciencia  ', tag: ' salud ', categories: ['ORDENADORES', 'TELEFONO_INTELIGENTE'], accessMode: 'PUBLIC', page: 2 });
    const { url, options } = lastRequest();
    expect(url.pathname).toBe('/public/surveys/page');
    expect(Object.fromEntries(url.searchParams)).toEqual({ search: 'ciencia', tag: 'salud', category: 'TELEFONO_INTELIGENTE', accessMode: 'PUBLIC', page: '2', size: '9' });
    expect(url.searchParams.getAll('category')).toEqual(['ORDENADORES', 'TELEFONO_INTELIGENTE']);
    expect(options).toEqual({ method: 'GET' });
  });

  it('ordena las encuestas propias y transmite la señal de cancelación', () => {
    const signal = new AbortController().signal;
    getMySurveys({ search: '  clima ', lifecycleStatus: 'OPEN', accessModes: ['PUBLIC', 'AUTHENTICATED'], categories: ['ORDENADORES', 'TELEFONO_INTELIGENTE'], sortOrder: 'OLDEST', signal });
    const { url, options } = lastRequest();
    expect(url.searchParams.get('search')).toBe('clima');
    expect(url.searchParams.get('lifecycleStatus')).toBe('OPEN');
    expect(url.searchParams.getAll('accessMode')).toEqual(['PUBLIC', 'AUTHENTICATED']);
    expect(url.searchParams.getAll('category')).toEqual(['ORDENADORES', 'TELEFONO_INTELIGENTE']);
    expect(url.searchParams.get('sort')).toBe('updatedAt,asc');
    expect(options.signal).toBe(signal);
  });

  it('envía los filtros de resultados y conserva el orden solicitado', () => {
    getResultSurveys({ searchFields: ['TITLE', 'OWNER'], lifecycleStatuses: ['OPEN', 'CLOSED'], sortOrder: 'OLDEST', page: 1, size: 18 });
    const { url } = lastRequest();
    expect(url.pathname).toBe('/surveys/results');
    expect(url.searchParams.getAll('lifecycleStatus')).toEqual(['OPEN', 'CLOSED']);
    expect(url.searchParams.get('searchFields')).toBe('TITLE,OWNER');
    expect(url.searchParams.get('sortOrder')).toBe('OLDEST');
    expect(url.searchParams.get('page')).toBe('1');
    expect(url.searchParams.get('size')).toBe('18');
  });

  it('omite filtros no seleccionados en el directorio y la supervisión', () => {
    getSurveyDirectory();
    expect(lastRequest().url.searchParams.has('lifecycleStatus')).toBe(false);
    getAdminSurveys({ visibilities: ['LISTED', 'UNLISTED'], lifecycleStatuses: ['DRAFT', 'OPEN'] });
    const { url } = lastRequest();
    expect(url.pathname).toBe('/admin/surveys');
    expect(url.searchParams.getAll('visibility')).toEqual(['LISTED', 'UNLISTED']);
    expect(url.searchParams.getAll('lifecycleStatus')).toEqual(['DRAFT', 'OPEN']);
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

  it('sube una portada como formulario multipart', () => {
    const file = new File(['imagen'], 'portada.png', { type: 'image/png' });
    uploadSurveyCover(12, file);
    const { url, options } = lastRequest();
    expect(url.pathname).toBe('/surveys/12/cover');
    expect(options.method).toBe('POST');
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

  it('envía la eliminación de encuesta al endpoint del propietario', () => {
    deleteSurvey(12);
    expect(apiClient).toHaveBeenLastCalledWith('/surveys/12', { method: 'DELETE' });
  });
});
