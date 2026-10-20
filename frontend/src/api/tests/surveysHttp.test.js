import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { HttpResponse, http } from 'msw';

import { getPublicSurveyById, getPublicSurveys } from '../surveysApi.js';
import { server } from '../../testing/mocks/server.js';

const baseUrl = 'http://localhost:8080/api/v1';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('consultas HTTP de encuestas públicas', () => {
  it('envía los filtros y lee el resultado del backend', async () => {
    server.use(http.get(`${baseUrl}/public/surveys/page`, ({ request }) => {
      const url = new URL(request.url);
      expect(url.searchParams.get('search')).toBe('salud');
      expect(url.searchParams.get('page')).toBe('1');
      return HttpResponse.json({ content: [{ id: 12, title: 'Hábitos digitales' }], totalElements: 1 });
    }));
    await expect(getPublicSurveys({ search: ' salud ', page: 1 })).resolves.toMatchObject({
      content: [{ id: 12, title: 'Hábitos digitales' }],
    });
  });

  it('propaga el error HTTP con el mensaje recibido', async () => {
    server.use(http.get(`${baseUrl}/public/surveys/12`, () => HttpResponse.json({ message: 'Encuesta cerrada' }, { status: 403 })));
    await expect(getPublicSurveyById(12)).rejects.toMatchObject({ status: 403, message: 'Encuesta cerrada' });
  });
});
