import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from './apiClient.js';
import {
  createCatalogBlock,
  createCatalogQuestion,
  createCatalogTemplate,
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlockById,
  getCatalogBlocks,
  getCatalogBlocksForBuilder,
  getCatalogQuestionById,
  getCatalogQuestions,
  getCatalogQuestionsForBuilder,
  getCatalogTemplateById,
  getCatalogTemplates,
  updateCatalogBlock,
  updateCatalogQuestion,
  updateCatalogTemplate,
} from './catalogApi.js';

vi.mock('./apiClient.js', () => ({ apiClient: vi.fn() }));

describe('catalogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.mockResolvedValue({ content: [] });
  });

  it('serializa los filtros de búsqueda y comparte las páginas cargadas', async () => {
    const options = {
      page: 7,
      size: 20,
      mine: true,
      search: '  salud  ',
      searchFields: ['TITLE', 'DESCRIPTION'],
      sortOrder: 'NEWEST',
      category: 'social',
    };
    await getCatalogQuestions(options);
    await getCatalogQuestions(options);
    expect(apiClient).toHaveBeenCalledTimes(1);
    const [path, request] = apiClient.mock.calls[0];
    const url = new URL(path, 'https://example.test');
    expect(url.pathname).toBe('/catalog/questions');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: '7', size: '20', mine: 'true', search: 'salud', searchFields: 'TITLE,DESCRIPTION',
      sortOrder: 'NEWEST', category: 'social',
    });
    expect(request).toEqual({ method: 'GET' });
    await getCatalogQuestions({ ...options, refresh: true });
    expect(apiClient).toHaveBeenCalledTimes(2);
  });

  it('permite cancelar una lectura sin cancelar la respuesta compartida en caché', async () => {
    const controller = new AbortController();
    const options = { page: 31, searchFields: [] };
    const pending = getCatalogBlocks({ ...options, signal: controller.signal });
    const shared = getCatalogBlocks(options);
    controller.abort();
    await expect(pending).rejects.toMatchObject({ name: 'AbortError' });
    await expect(shared).resolves.toEqual({ content: [] });
    expect(apiClient).toHaveBeenCalledTimes(1);
    const url = new URL(apiClient.mock.calls[0][0], 'https://example.test');
    expect(url.searchParams.get('searchFields')).toBe('NONE');
    await expect(getCatalogBlocks({ ...options, signal: controller.signal })).rejects.toMatchObject({ name: 'AbortError' });
  });

  it('expulsa una petición fallida y vuelve a solicitarla en el siguiente intento', async () => {
    apiClient.mockRejectedValueOnce(new Error('Sin conexión'));
    await expect(getCatalogTemplates({ page: 19 })).rejects.toThrow('Sin conexión');
    await expect(getCatalogTemplates({ page: 19 })).resolves.toEqual({ content: [] });
    expect(apiClient).toHaveBeenCalledTimes(2);
  });

  it('invalida preguntas al modificarlas y transmite el cuerpo de los cambios', async () => {
    await getCatalogQuestionsForBuilder({ page: 12 });
    const question = { statement: '¿Edad?' };
    await createCatalogQuestion(question);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/questions', { method: 'POST', body: question });
    await getCatalogQuestionsForBuilder({ page: 12 });
    expect(apiClient).toHaveBeenCalledTimes(3);
    await updateCatalogQuestion(4, question);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/questions/4', { method: 'PUT', body: question });
  });

  it('usa las rutas de detalle y limpia las páginas de bloques y plantillas tras mutaciones', async () => {
    await getCatalogBlocks({ page: 23 });
    await getCatalogTemplates({ page: 23 });
    await getCatalogBlockById(5);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/blocks/5', { method: 'GET' });
    await getCatalogTemplateById(6);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/templates/6', { method: 'GET' });
    await createCatalogBlock({ name: 'Bloque' });
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/blocks', { method: 'POST', body: { name: 'Bloque' } });
    await deleteCatalogTemplate(6);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/templates/6', { method: 'DELETE' });
    await getCatalogBlocks({ page: 23 });
    await getCatalogTemplates({ page: 23 });
    expect(apiClient).toHaveBeenCalledTimes(8);
  });

  it('invalida las páginas correspondientes tras editar o borrar elementos', async () => {
    await getCatalogQuestionById(4);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/questions/4', { method: 'GET' });
    await getCatalogBlocksForBuilder({ page: 5 });
    await updateCatalogBlock(5, { name: 'Revisado' });
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/blocks/5', { method: 'PUT', body: { name: 'Revisado' } });
    await getCatalogBlocksForBuilder({ page: 5 });
    await deleteCatalogBlock(5);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/blocks/5', { method: 'DELETE' });
    await deleteCatalogQuestion(4);
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/questions/4', { method: 'DELETE' });
    await createCatalogTemplate({ title: 'Nueva' });
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/templates', { method: 'POST', body: { title: 'Nueva' } });
    await updateCatalogTemplate(8, { title: 'Revisada' });
    expect(apiClient).toHaveBeenLastCalledWith('/catalog/templates/8', { method: 'PUT', body: { title: 'Revisada' } });
  });

  it('envía la categoría al consultar preguntas y bloques del constructor', async () => {
    await getCatalogQuestionsForBuilder({ page: 0, category: 'SALUD' });
    await getCatalogBlocksForBuilder({ page: 1, category: 'SALUD' });

    const [questionsPath, blocksPath] = apiClient.mock.calls.map(([path]) => path);
    expect(new URL(questionsPath, 'https://example.test').searchParams.get('category')).toBe('SALUD');
    expect(new URL(blocksPath, 'https://example.test').searchParams.get('category')).toBe('SALUD');
    expect(new URL(blocksPath, 'https://example.test').searchParams.get('page')).toBe('1');
  });
});
