import { fireEvent, screen } from '@testing-library/react';
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
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: ({ onAddQuestion, onAddBlock }) => <><button onClick={() => onAddQuestion({ id: 3, statement: 'Añadida', questionType: 'TEXT', settings: { maxLength: 300 } })}>Añadir del catálogo</button>{onAddBlock && <button onClick={() => onAddBlock({ id: 4, name: 'Referencia' }, 0)}>Añadir bloque del catálogo</button>}</> }));
vi.mock('../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange }) => (
    <label>Enunciado
      <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange, onDropQuestionFromSidebar }) => (
    <>
      <button onClick={() => onDropQuestionFromSidebar({ id: 3, statement: 'Insertada', questionType: 'TEXT', settings: { maxLength: 300 } }, 0)}>Insertar pregunta</button>
      <label>Nombre del bloque<input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} /></label>
    </>
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

  it.each(['bloque', 'plantilla'])('incorpora preguntas y bloques del catálogo al editar %s', async type => {
    renderPage(type);
    fireEvent.click(await screen.findByRole('button', { name: 'Añadir del catálogo' }));
    if (type === 'bloque') fireEvent.click(screen.getByRole('button', { name: 'Insertar pregunta' }));
    else fireEvent.click(screen.getByRole('button', { name: 'Añadir bloque del catálogo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('status')).toBeVisible();
    const update = type === 'bloque' ? updateCatalogBlock : updateCatalogTemplate;
    expect(update).toHaveBeenCalledWith('12', expect.objectContaining({ items: expect.arrayContaining([expect.objectContaining({ itemType: 'QUESTION' })]) }));
  });
  it('reutiliza el elemento de la vista de detalles y conserva su categoría al guardar', async () => {
    const element = { id: 12, statement: 'Reutilizada', questionType: 'TEXT', settings: { maxLength: 300 }, category: 'SALUD' };
    renderWithDataRouter([{ path: '/catalogo/editar/:elementType/:elementId', element: <EditElementPage /> }], {
      route: { pathname: '/catalogo/editar/pregunta/12', state: { element, elementId: 12, elementType: 'pregunta', fromDetails: true } },
    });
    expect(screen.getByRole('textbox', { name: 'Enunciado' })).toHaveValue('Reutilizada');
    expect(getCatalogQuestionById).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('status')).toBeVisible();
    expect(updateCatalogQuestion).toHaveBeenCalledWith('12', expect.objectContaining({ category: 'SALUD' }));
  });
  it.each([new Error('No se pudo guardar'), {}])('conserva la edición tras un error al guardar', async error => {
    updateCatalogQuestion.mockRejectedValue(error);
    renderPage('pregunta');
    await screen.findByRole('textbox', { name: 'Enunciado' });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(error.message || 'No se ha podido actualizar la pregunta fija.');
  });
  it('muestra el error de carga por defecto', async () => {
    getCatalogQuestionById.mockRejectedValue({});
    renderPage('pregunta');
    expect(await screen.findByRole('alert')).toHaveTextContent('No se ha podido cargar la pregunta fija.');
  });
  it('redirecciona tipos de elemento desconocidos al catálogo', async () => {
    renderWithDataRouter([
      { path: '/catalogo/editar/:elementType/:elementId', element: <EditElementPage /> },
      { path: '/catalogo', element: <p>Catálogo seguro</p> },
    ], { route: '/catalogo/editar/desconocido/12' });
    expect(await screen.findByText('Catálogo seguro')).toBeVisible();
  });
});
