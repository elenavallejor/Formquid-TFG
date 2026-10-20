import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  getCatalogBlockById,
  getCatalogQuestionById,
  getCatalogTemplateById,
  updateCatalogBlock,
  updateCatalogQuestion,
  updateCatalogTemplate,
} from '../../../api/catalogApi.js';
import { EditElementPage } from './EditElementPage.jsx';
import { renderWithDataRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/catalogApi.js', () => ({
  getCatalogBlockById: vi.fn(), getCatalogQuestionById: vi.fn(), getCatalogTemplateById: vi.fn(),
  updateCatalogBlock: vi.fn(), updateCatalogQuestion: vi.fn(), updateCatalogTemplate: vi.fn(),
}));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([])) }));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));
vi.mock('../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange }) => (
    <label>Enunciado
      <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange }) => (
    <label>Nombre del bloque
      <input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../components/TemplateBuilder/TemplateBuilder.jsx', () => ({
  TemplateBuilder: ({ data, onChange }) => (
    <label>Título de plantilla
      <input value={data.title} onChange={(event) => onChange({ ...data, title: event.target.value })} />
    </label>
  ),
}));

function renderPage(type) {
  renderWithDataRouter(
    [{ path: '/catalogo/editar/:elementType/:elementId', element: <EditElementPage /> }],
    { route: `/catalogo/editar/${type}/12` },
  );
}

describe('EditElementPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCatalogQuestionById.mockResolvedValue({ id: 12, statement: 'Pregunta actual', questionType: 'TEXT', settings: { maxLength: 300 } });
    getCatalogBlockById.mockResolvedValue({ id: 12, name: 'Bloque actual', contentItems: [] });
    getCatalogTemplateById.mockResolvedValue({ id: 12, title: 'Plantilla actual', contentItems: [] });
  });

  it('carga y guarda los cambios de una pregunta fija', async () => {
    const user = userEvent.setup();
    updateCatalogQuestion.mockResolvedValue({ id: 12 });
    renderPage('pregunta');
    const statement = await screen.findByRole('textbox', { name: 'Enunciado' });
    await user.clear(statement);
    await user.type(statement, 'Pregunta nueva');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Pregunta fija actualizada con éxito.');
    expect(updateCatalogQuestion).toHaveBeenCalledWith('12', expect.objectContaining({
      question: expect.objectContaining({ statement: 'Pregunta nueva' }),
    }));
  });

  it('valida el contenido de un bloque antes de actualizarlo', async () => {
    const user = userEvent.setup();
    renderPage('bloque');
    expect(await screen.findByRole('textbox', { name: 'Nombre del bloque' })).toHaveValue('Bloque actual');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(screen.getByRole('alert')).toHaveTextContent('El bloque debe contener al menos una pregunta.');
    expect(updateCatalogBlock).not.toHaveBeenCalled();
  });

  it('actualiza una plantilla existente', async () => {
    const user = userEvent.setup();
    updateCatalogTemplate.mockResolvedValue({ id: 12 });
    renderPage('plantilla');
    const title = await screen.findByRole('textbox', { name: 'Título de plantilla' });
    await user.clear(title);
    await user.type(title, 'Plantilla revisada');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Plantilla actualizada con éxito.');
    expect(updateCatalogTemplate).toHaveBeenCalledWith('12', expect.objectContaining({ title: 'Plantilla revisada' }));
  });

  it('muestra el error de carga de una pregunta fija', async () => {
    getCatalogQuestionById.mockRejectedValue(new Error('Elemento no disponible'));
    renderPage('pregunta');
    expect(await screen.findByRole('alert')).toHaveTextContent('Elemento no disponible');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeDisabled();
  });
});
