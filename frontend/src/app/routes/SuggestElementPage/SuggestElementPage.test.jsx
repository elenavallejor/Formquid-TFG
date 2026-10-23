import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createElementRequest } from '../../../api/elementRequestsApi.js';
import { SuggestElementPage } from './SuggestElementPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/elementRequestsApi.js', () => ({ createElementRequest: vi.fn() }));
vi.mock('../../../api/categoriesApi.js', () => ({
  getCategories: vi.fn(() => Promise.resolve([
    { code: 'SALUD', name: 'Salud' },
    { code: 'OTROS', name: 'Otros' },
  ])),
}));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({
  CatalogSidebar: ({ categoryFilter, onAddQuestion }) => (
    <div data-testid="catalog-sidebar" data-category={categoryFilter}>
      <button type="button" onClick={() => onAddQuestion({ id: 5, category: 'SALUD', statement: 'Salud', questionType: 'TEXT', scope: 'CATALOG_FIXED' })}>Añadir pregunta de Salud</button>
      <button type="button" onClick={() => onAddQuestion({ id: 6, category: 'OTROS', statement: 'Otra', questionType: 'TEXT', scope: 'CATALOG_FIXED' })}>Añadir pregunta de Otros</button>
    </div>
  ),
}));
vi.mock('../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange, afterPrimaryField }) => (
    <section aria-label="Datos de la pregunta">
      <label>Enunciado
        <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
      </label>
      {afterPrimaryField}
    </section>
  ),
}));
vi.mock('../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange, afterPrimaryField }) => (
    <section aria-label="Datos del bloque">
      <label>Nombre del bloque
        <input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} />
      </label>
      {afterPrimaryField}
    </section>
  ),
}));
vi.mock('../../../components/TemplateBuilder/TemplateBuilder.jsx', () => ({
  TemplateBuilder: ({ data, onChange, afterPrimaryField }) => (
    <section aria-label="Datos de la plantilla">
      <label>Título de plantilla
        <input value={data.title} onChange={(event) => onChange({ ...data, title: event.target.value })} />
      </label>
      {afterPrimaryField}
    </section>
  ),
}));

async function selectCategory(user) {
  await user.click(screen.getByRole('button', { name: 'Categoría (obligatoria)' }));
  fireEvent.click(await screen.findByRole('button', { name: 'Salud' }));
}

describe('SuggestElementPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('requiere un motivo y un enunciado antes de enviar una pregunta', async () => {
    const user = userEvent.setup();
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Pregunta fija.*Propón una nueva pregunta/ }));
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Explica brevemente el motivo');
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), 'Es útil');
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes redactar el enunciado');
    expect(createElementRequest).not.toHaveBeenCalled();
  });

  it('envía una propuesta válida de pregunta fija', async () => {
    const user = userEvent.setup();
    createElementRequest.mockResolvedValue({ id: 4 });
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Pregunta fija.*Propón una nueva pregunta/ }));
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), '  Nueva temática  ');
    await user.type(screen.getByRole('textbox', { name: 'Enunciado' }), '¿Qué opinas?');
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una categoría');
    expect(createElementRequest).not.toHaveBeenCalled();
    await selectCategory(user);
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud enviada con éxito.');
    expect(createElementRequest).toHaveBeenCalledWith(expect.objectContaining({
      elementType: 'FIXED_QUESTION',
      message: 'Nueva temática',
      requestPayload: expect.objectContaining({ category: 'SALUD', question: expect.objectContaining({ statement: '¿Qué opinas?' }) }),
    }));
  });

  it('permite cambiar de tipo y muestra fallos del servidor', async () => {
    const user = userEvent.setup();
    createElementRequest.mockRejectedValue(new Error('Servicio no disponible'));
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Bloque.*Agrupa preguntas/ }));
    expect(screen.getByRole('textbox', { name: 'Nombre del bloque' })).toBeVisible();
    expect(within(screen.getByRole('region', { name: 'Datos del bloque' })).getByRole('button', { name: 'Categoría (obligatoria)' })).toHaveTextContent('Selecciona la categoría de tu bloque');
    await user.click(screen.getByRole('button', { name: /Plantilla.*Combina preguntas/ }));
    expect(within(screen.getByRole('region', { name: 'Datos de la plantilla' })).getByRole('button', { name: 'Categoría (obligatoria)' })).toHaveTextContent('Selecciona la categoría de tu plantilla');
    await user.click(screen.getByRole('button', { name: /Pregunta fija.*Propón una nueva pregunta/ }));
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), 'Hace falta');
    await user.type(screen.getByRole('textbox', { name: 'Enunciado' }), 'Pregunta general');
    await selectCategory(user);
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(createElementRequest).toHaveBeenCalledWith(expect.objectContaining({ elementType: 'FIXED_QUESTION' }));
  });

  it('filtra el catálogo y rechaza elementos de otra categoría', async () => {
    const user = userEvent.setup();
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Bloque.*Agrupa preguntas/ }));
    expect(screen.getByTestId('catalog-sidebar')).toHaveAttribute('data-category', '');
    await selectCategory(user);
    expect(screen.getByTestId('catalog-sidebar')).toHaveAttribute('data-category', 'SALUD');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de Otros' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Solo puedes añadir preguntas del catálogo de la categoría seleccionada');
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de Salud' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Categoría (obligatoria)' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Otros' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Quita primero los elementos añadidos del catálogo');
    expect(screen.getByTestId('catalog-sidebar')).toHaveAttribute('data-category', 'SALUD');
  });

  it.each([
    { type: 'Bloque', field: 'Nombre del bloque', value: 'Bloque de salud', elementType: 'BLOCK' },
    { type: 'Plantilla', field: 'Título de plantilla', value: 'Plantilla de salud', elementType: 'TEMPLATE' },
  ])('envía la categoría de una propuesta de $type', async ({ type, field, value, elementType }) => {
    const user = userEvent.setup();
    createElementRequest.mockResolvedValue({ id: 8 });
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: new RegExp(`^${type}`) }));
    await user.type(screen.getByRole('textbox', { name: field }), value);
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), 'Propuesta de salud');
    await selectCategory(user);
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta de Salud' }));
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud enviada con éxito.');
    expect(createElementRequest).toHaveBeenCalledWith(expect.objectContaining({
      elementType,
      requestPayload: expect.objectContaining({ category: 'SALUD' }),
    }));
  });
});
