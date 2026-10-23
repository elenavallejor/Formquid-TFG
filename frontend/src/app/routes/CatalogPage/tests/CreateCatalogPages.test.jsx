import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createCatalogBlock, createCatalogQuestion, createCatalogTemplate } from '../../../../api/catalogApi.js';
import { CreateBlockPage } from '../../CreateBlockPage/CreateBlockPage.jsx';
import { CreateQuestionPage } from '../../CreateQuestionPage/CreateQuestionPage.jsx';
import { CreateTemplatePage } from '../../CreateTemplatePage/CreateTemplatePage.jsx';
import { renderWithDataRouter } from '../../../../testing/render.jsx';

vi.mock('../../../../api/catalogApi.js', () => ({
  createCatalogBlock: vi.fn(),
  createCatalogQuestion: vi.fn(),
  createCatalogTemplate: vi.fn(),
}));
vi.mock('../../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([{ code: 'SALUD', name: 'Salud' }, { code: 'TECNOLOGIA', name: 'Tecnología' }])) }));
vi.mock('../../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({
  CatalogSidebar: ({ onAddQuestion, onAddBlock }) => (
    <aside>
      <button type="button" onClick={() => onAddQuestion({ id: 4, scope: 'CATALOG_FIXED', category: 'SALUD', questionType: 'TEXT', statement: 'Pregunta fija', settings: { maxLength: 300 } })}>Añadir pregunta fija</button>
      <button type="button" onClick={() => onAddQuestion({ id: 6, scope: 'CATALOG_FIXED', category: 'TECNOLOGIA', questionType: 'TEXT', statement: 'Pregunta de tecnología', settings: { maxLength: 300 } })}>Añadir pregunta de otra categoría</button>
      {onAddBlock && <button type="button" onClick={() => onAddBlock({ id: 5, category: 'SALUD', name: 'Bloque fijo', contentItems: [] })}>Añadir bloque fijo</button>}
    </aside>
  ),
}));
vi.mock('../../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange, afterPrimaryField }) => (
    <><label>Enunciado
      <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
    </label>{afterPrimaryField}</>
  ),
}));
vi.mock('../../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange, afterPrimaryField }) => (
    <>
      <label>Nombre del bloque
        <input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} />
      </label>
      {afterPrimaryField}
      <button type="button" onClick={() => onChange({
        ...data,
        items: [{ itemType: 'QUESTION', statement: 'Pregunta del bloque', questionType: 'TEXT', maxLength: 300 }],
      })}>Añadir pregunta de prueba</button>
    </>
  ),
}));
vi.mock('../../../../components/TemplateBuilder/TemplateBuilder.jsx', () => ({
  TemplateBuilder: ({ data, onChange, afterPrimaryField }) => (
    <><label>Título de la plantilla
      <input value={data.title} onChange={(event) => onChange({ ...data, title: event.target.value })} />
    </label>{afterPrimaryField}</>
  ),
}));

function renderCreatePage(Page) {
  return renderWithDataRouter([
    { path: '/', element: <Page /> },
    { path: '/panel', element: <h1>Panel</h1> },
  ]);
}

async function chooseCategory(user, name = 'Salud') {
  await user.click(screen.getByRole('button', { name: 'Categoría (obligatoria)' }));
  fireEvent.click(await screen.findByRole('button', { name }));
}

describe('pantallas de creación del catálogo', () => {
  beforeEach(() => vi.resetAllMocks());

  it('valida el enunciado antes de crear una pregunta fija', async () => {
    const user = userEvent.setup();
    renderCreatePage(CreateQuestionPage);
    await user.click(screen.getByRole('button', { name: 'Crear pregunta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes redactar el enunciado de la pregunta.');
    expect(createCatalogQuestion).not.toHaveBeenCalled();
  });

  it('crea una pregunta fija válida y confirma el guardado', async () => {
    const user = userEvent.setup();
    createCatalogQuestion.mockResolvedValue({ id: 4 });
    renderCreatePage(CreateQuestionPage);
    await user.type(screen.getByRole('textbox', { name: 'Enunciado' }), '¿Qué opinas?');
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Crear pregunta' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Pregunta fija creada con éxito.');
    expect(window.dispatchEvent(new Event('beforeunload', { cancelable: true }))).toBe(true);
    expect(createCatalogQuestion).toHaveBeenCalledWith(expect.objectContaining({
      question: expect.objectContaining({ statement: '¿Qué opinas?' }),
    }));
  });

  it('muestra un error si falla el guardado del bloque', async () => {
    const user = userEvent.setup();
    createCatalogBlock.mockRejectedValue(new Error('Servidor no disponible'));
    renderCreatePage(CreateBlockPage);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque inicial');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de prueba' }));
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor no disponible');
    expect(createCatalogBlock).toHaveBeenCalledWith(expect.objectContaining({ name: 'Bloque inicial' }));
  });

  it('exige un nombre y una pregunta antes de crear un bloque', async () => {
    const user = userEvent.setup();
    renderCreatePage(CreateBlockPage);
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
    renderCreatePage(CreateBlockPage);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque inicial');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de prueba' }));
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Crear bloque' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Bloque creado con éxito');
    expect(createCatalogBlock).toHaveBeenCalledOnce();
  });

  it('añade una pregunta del catálogo a un bloque antes de guardarlo', async () => {
    const user = userEvent.setup();
    createCatalogBlock.mockResolvedValue({ id: 12 });
    renderCreatePage(CreateBlockPage);
    await user.type(screen.getByRole('textbox', { name: 'Nombre del bloque' }), 'Bloque con catálogo');
    await chooseCategory(user);
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
    renderCreatePage(CreateTemplatePage);
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes asignar un título a la plantilla.');
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla general');
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Plantilla creada con éxito.');
    expect(createCatalogTemplate).toHaveBeenCalledWith(expect.objectContaining({ title: 'Plantilla general' }));
  });

  it('conserva el formulario si el backend rechaza la plantilla', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockRejectedValue(new Error('Plantilla duplicada'));
    renderCreatePage(CreateTemplatePage);
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla general');
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Plantilla duplicada');
    expect(screen.getByRole('textbox', { name: 'Título de la plantilla' })).toHaveValue('Plantilla general');
  });

  it('incorpora elementos del catálogo en una plantilla', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockResolvedValue({ id: 9 });
    renderCreatePage(CreateTemplatePage);
    await user.type(screen.getByRole('textbox', { name: 'Título de la plantilla' }), 'Plantilla con catálogo');
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta fija' }));
    await user.click(screen.getByRole('button', { name: 'Añadir bloque fijo' }));
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Plantilla creada con éxito.');
    expect(createCatalogTemplate).toHaveBeenCalledWith(expect.objectContaining({ title: 'Plantilla con catálogo' }));
  });

  it.each([
    ['pregunta fija', CreateQuestionPage, 'Enunciado', 'Crear pregunta'],
    ['bloque', CreateBlockPage, 'Nombre del bloque', 'Crear bloque'],
    ['plantilla', CreateTemplatePage, 'Título de la plantilla', 'Guardar plantilla'],
  ])('exige elegir una categoría antes de guardar una %s', async (elementName, Page, fieldName, saveLabel) => {
    const user = userEvent.setup();
    const categoryMessage = `Selecciona una categoría para ${elementName === 'bloque' ? 'el' : 'la'} ${elementName}.`;
    renderCreatePage(Page);
    await user.type(screen.getByRole('textbox', { name: fieldName }), 'Elemento válido');
    if (elementName === 'bloque') await user.click(screen.getByRole('button', { name: 'Añadir pregunta de prueba' }));

    await user.click(screen.getByRole('button', { name: saveLabel }));
    expect(screen.getByRole('alert')).toHaveTextContent(categoryMessage);
    expect(screen.getByRole('button', { name: 'Categoría (obligatoria)' })).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Categoría (obligatoria)' })).toHaveFocus();
    expect(createCatalogQuestion).not.toHaveBeenCalled();
    expect(createCatalogBlock).not.toHaveBeenCalled();
    expect(createCatalogTemplate).not.toHaveBeenCalled();

    await chooseCategory(user);
    expect(screen.queryByText(categoryMessage)).not.toBeInTheDocument();
  });

  it.each([
    ['bloque', CreateBlockPage],
    ['plantilla', CreateTemplatePage],
  ])('impide cambiar la categoría de un %s con elementos importados del catálogo', async (elementName, Page) => {
    const user = userEvent.setup();
    renderCreatePage(Page);
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta fija' }));
    await chooseCategory(user, 'Tecnología');

    expect(screen.getByRole('button', { name: 'Categoría (obligatoria)' })).toHaveTextContent('Salud');
    expect(screen.getByRole('alert')).toHaveTextContent('Quita primero');
    expect(screen.getByRole('alert')).toHaveTextContent(elementName);
  });

  it.each([
    ['bloque', CreateBlockPage],
    ['plantilla', CreateTemplatePage],
  ])('rechaza en un %s una pregunta de otra categoría', async (_elementName, Page) => {
    const user = userEvent.setup();
    renderCreatePage(Page);
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de otra categoría' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Solo puedes añadir preguntas del catálogo de la categoría seleccionada.');
  });

  it.each([
    ['pregunta fija', CreateQuestionPage, 'Enunciado'],
    ['bloque', CreateBlockPage, 'Nombre del bloque'],
    ['plantilla', CreateTemplatePage, 'Título de la plantilla'],
  ])('avisa antes de salir de una %s modificada con el estilo de encuestas', async (elementName, Page, fieldName) => {
    const user = userEvent.setup();
    const { router } = renderCreatePage(Page);

    await user.type(screen.getByRole('textbox', { name: fieldName }), 'Cambio sin guardar');
    expect(window.dispatchEvent(new Event('beforeunload', { cancelable: true }))).toBe(false);
    await user.click(screen.getByRole('link', { name: 'PANEL' }));

    const dialog = screen.getByRole('dialog', { name: 'Salir sin guardar' });
    expect(dialog).toHaveTextContent(`tu ${elementName} no se guardará`);
    expect(screen.getByRole('button', { name: 'Salir sin guardar' })).toHaveClass('confirm-modal-confirm-danger');
    expect(router.state.location.pathname).toBe('/');

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('dialog', { name: 'Salir sin guardar' })).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: fieldName })).toHaveValue('Cambio sin guardar');

    await user.click(screen.getByRole('link', { name: 'PANEL' }));
    await user.click(screen.getByRole('button', { name: 'Salir sin guardar' }));
    expect(await screen.findByRole('heading', { name: 'Panel' })).toBeVisible();
    expect(router.state.location.pathname).toBe('/panel');
  });

  it.each([
    ['pregunta fija', CreateQuestionPage],
    ['bloque', CreateBlockPage],
    ['plantilla', CreateTemplatePage],
  ])('permite salir de una %s sin cambios directamente', async (_elementName, Page) => {
    const user = userEvent.setup();
    renderCreatePage(Page);
    await user.click(screen.getByRole('link', { name: 'PANEL' }));
    expect(await screen.findByRole('heading', { name: 'Panel' })).toBeVisible();
    expect(screen.queryByRole('dialog', { name: 'Salir sin guardar' })).not.toBeInTheDocument();
  });

  it.each([
    ['bloque', CreateBlockPage],
    ['plantilla', CreateTemplatePage],
  ])('considera cambios los elementos añadidos desde el catálogo a un %s', async (_elementName, Page) => {
    const user = userEvent.setup();
    renderCreatePage(Page);
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta fija' }));
    await user.click(screen.getByRole('link', { name: 'PANEL' }));
    expect(screen.getByRole('dialog', { name: 'Salir sin guardar' })).toBeVisible();
  });
});
