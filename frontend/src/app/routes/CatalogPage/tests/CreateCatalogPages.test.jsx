import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createCatalogBlock, createCatalogQuestion, createCatalogTemplate } from '../../../../api/catalogApi.js';
import { CreateBlockPage } from '../../CreateBlockPage/CreateBlockPage.jsx';
import { CreateQuestionPage } from '../../CreateQuestionPage/CreateQuestionPage.jsx';
import { CreateTemplatePage } from '../../CreateTemplatePage/CreateTemplatePage.jsx';
import { renderWithRouter } from '../../../../testing/render.jsx';

vi.mock('../../../../api/catalogApi.js', () => ({
  createCatalogBlock: vi.fn(),
  createCatalogQuestion: vi.fn(),
  createCatalogTemplate: vi.fn(),
}));
vi.mock('../../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([])) }));
vi.mock('../../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({
  CatalogSidebar: ({ onAddQuestion, onAddBlock }) => (
    <aside>
      <button type="button" onClick={() => onAddQuestion({ id: 4, questionType: 'TEXT', statement: 'Pregunta fija', settings: { maxLength: 300 } })}>Añadir pregunta fija</button>
      {onAddBlock && <button type="button" onClick={() => onAddBlock({ id: 5, name: 'Bloque fijo', contentItems: [] })}>Añadir bloque fijo</button>}
    </aside>
  ),
}));
vi.mock('../../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange }) => (
    <label>Enunciado
      <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange }) => (
    <>
      <label>Nombre del bloque
        <input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} />
      </label>
      <button type="button" onClick={() => onChange({
        ...data,
        items: [{ itemType: 'QUESTION', statement: 'Pregunta del bloque', questionType: 'TEXT', maxLength: 300 }],
      })}>Añadir pregunta de prueba</button>
    </>
  ),
}));
vi.mock('../../../../components/TemplateBuilder/TemplateBuilder.jsx', () => ({
  TemplateBuilder: ({ data, onChange }) => (
    <label>Título de la plantilla
      <input value={data.title} onChange={(event) => onChange({ ...data, title: event.target.value })} />
    </label>
  ),
}));

describe('pantallas de creación del catálogo', () => {
  beforeEach(() => vi.resetAllMocks());

  it('valida el enunciado antes de crear una pregunta fija', async () => {
    const user = userEvent.setup();
    renderWithRouter(<CreateQuestionPage />);
    await user.click(screen.getByRole('button', { name: 'Crear pregunta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes redactar el enunciado de la pregunta.');
    expect(createCatalogQuestion).not.toHaveBeenCalled();
  });

  it('crea una pregunta fija válida y confirma el guardado', async () => {
    const user = userEvent.setup();
    createCatalogQuestion.mockResolvedValue({ id: 4 });
    renderWithRouter(<CreateQuestionPage />);
    await user.type(screen.getByRole('textbox', { name: 'Enunciado' }), '¿Qué opinas?');
    await user.click(screen.getByRole('button', { name: 'Crear pregunta' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Pregunta fija creada con éxito.');
    expect(createCatalogQuestion).toHaveBeenCalledWith(expect.objectContaining({
      question: expect.objectContaining({ statement: '¿Qué opinas?' }),
    }));
  });

  it('muestra un error si falla el guardado del bloque', async () => {
    const user = userEvent.setup();
    createCatalogBlock.mockRejectedValue(new Error('Servidor no disponible'));
    renderWithRouter(<CreateBlockPage />);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque inicial');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de prueba' }));
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor no disponible');
    expect(createCatalogBlock).toHaveBeenCalledWith(expect.objectContaining({ name: 'Bloque inicial' }));
  });

  it('exige un nombre y una pregunta antes de crear un bloque', async () => {
    const user = userEvent.setup();
    renderWithRouter(<CreateBlockPage />);
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes asignar un nombre');
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque inicial');
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(screen.getByRole('alert')).toHaveTextContent('al menos una pregunta');
    expect(createCatalogBlock).not.toHaveBeenCalled();
  });

  it('confirma la creación de un bloque válido', async () => {
    const user = userEvent.setup();
    createCatalogBlock.mockResolvedValue({ id: 12 });
    renderWithRouter(<CreateBlockPage />);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque inicial');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de prueba' }));
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Bloque creado con éxito');
    expect(createCatalogBlock).toHaveBeenCalledOnce();
  });

  it('añade una pregunta del catálogo a un bloque antes de guardarlo', async () => {
    const user = userEvent.setup();
    createCatalogBlock.mockResolvedValue({ id: 12 });
    renderWithRouter(<CreateBlockPage />);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque con catálogo');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta fija' }));
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Bloque creado con éxito');
    expect(createCatalogBlock).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Bloque con catálogo',
    }));
  });

  it('valida y guarda una plantilla de catálogo', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockResolvedValue({ id: 9 });
    renderWithRouter(<CreateTemplatePage />);
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes asignar un título a la plantilla.');
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla general');
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Plantilla creada con éxito.');
    expect(createCatalogTemplate).toHaveBeenCalledWith(expect.objectContaining({ title: 'Plantilla general' }));
  });

  it('conserva el formulario si el backend rechaza la plantilla', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockRejectedValue(new Error('Plantilla duplicada'));
    renderWithRouter(<CreateTemplatePage />);
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla general');
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Plantilla duplicada');
    expect(screen.getByRole('textbox', { name: 'Título de la plantilla' })).toHaveValue('Plantilla general');
  });

  it('incorpora elementos del catálogo en una plantilla', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockResolvedValue({ id: 9 });
    renderWithRouter(<CreateTemplatePage />);
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla con catálogo');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta fija' }));
    await user.click(screen.getByRole('button', { name: 'Añadir bloque fijo' }));
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Plantilla creada con éxito.');
    expect(createCatalogTemplate).toHaveBeenCalledWith(expect.objectContaining({ title: 'Plantilla con catálogo' }));
  });
});
