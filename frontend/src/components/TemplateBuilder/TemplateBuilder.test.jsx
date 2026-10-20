import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { TemplateBuilder } from './TemplateBuilder.jsx';
import { INITIAL_TEMPLATE_STATE } from '../../utils/templateBuilderUtils.js';

vi.mock('../../api/catalogApi.js', () => ({
  getCatalogQuestionsForBuilder: vi.fn().mockResolvedValue({ content: [] }),
  getCatalogBlocksForBuilder: vi.fn().mockResolvedValue({ content: [] }),
}));

function Harness({ initial = INITIAL_TEMPLATE_STATE, readOnly = false }) {
  const [data, setData] = useState(initial);
  return <MemoryRouter><TemplateBuilder data={data} onChange={setData} readOnly={readOnly} /></MemoryRouter>;
}

async function chooseElement(user, name) {
  await user.click(screen.getByRole('button', { name: 'Crear elemento' }));
  const selector = screen.getByRole('dialog', { name: 'CREAR ELEMENTO' });
  await user.click(within(selector).getByRole('button', { name: new RegExp(name) }));
}

describe('TemplateBuilder', () => {
  it('edita la información general y gestiona el ciclo de vida de un texto', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(screen.getByRole('textbox', { name: /Título de la plantilla/ }), 'Plantilla de salud');
    expect(screen.getByRole('textbox', { name: /Título de la plantilla/ })).toHaveValue('Plantilla de salud');
    await chooseElement(user, 'Texto');
    let dialog = screen.getByRole('dialog', { name: 'AÑADIR TEXTO' });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('El texto no puede estar vacío.');
    await user.type(within(dialog).getByRole('textbox', { name: /Contenido/ }), 'Introducción');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(screen.getByText('Introducción')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Opciones de texto' }));
    await user.click(screen.getByRole('button', { name: 'Editar texto' }));
    dialog = screen.getByRole('dialog', { name: 'EDITAR TEXTO' });
    const textbox = within(dialog).getByRole('textbox', { name: /Contenido/ });
    await user.clear(textbox);
    await user.type(textbox, 'Introducción revisada');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(screen.getByText('Introducción revisada')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Opciones de texto' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar de la plantilla' }));
    dialog = screen.getByRole('dialog', { name: 'ELIMINAR TEXTO' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));
    expect(screen.queryByText('Introducción revisada')).not.toBeInTheDocument();
  });

  it('crea una pregunta propia y permite cambiar su obligatoriedad', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await chooseElement(user, 'Pregunta nueva');
    const dialog = screen.getByRole('dialog', { name: 'Crear pregunta propia' });
    fireEvent.change(within(dialog).getByPlaceholderText('Texto que verá el encuestado.'), { target: { value: '¿Cómo estás?' } });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));
    expect(screen.getByRole('heading', { name: /¿Cómo estás\?/ })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Opciones de pregunta' })).toBeVisible();
  });

  it('valida el bloque nuevo y oculta los controles de edición en solo lectura', async () => {
    const user = userEvent.setup();
    const view = render(<Harness />);
    await chooseElement(user, 'Bloque nuevo');
    const dialog = screen.getByRole('dialog', { name: 'CREAR BLOQUE' });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar bloque' }));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('Debes asignar un nombre al bloque.');
    view.unmount();
    render(<Harness initial={{ title: 'Plantilla', items: [{ itemType: 'TEXT', textBody: 'Solo lectura' }] }} readOnly />);
    expect(screen.getByText('Solo lectura')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Crear elemento' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Opciones de texto' })).not.toBeInTheDocument();
  });

  it('mueve elementos con los controles de teclado accesibles', async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ title: 'Plantilla', items: [
      { itemType: 'TEXT', textBody: 'Primero' },
      { itemType: 'TEXT', textBody: 'Segundo' },
    ] }} />);
    await user.click(screen.getByRole('button', { name: 'Mover texto 2 hacia arriba' }));
    expect(screen.getAllByText(/Primero|Segundo/).map((node) => node.textContent)).toEqual(['Segundo', 'Primero']);
  });

  it('mueve una pregunta propia dentro de un bloque y permite sacarla', async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ title: 'Plantilla', items: [
      { itemType: 'QUESTION', questionType: 'TEXT', statement: 'Pregunta propia', localId: 'q-1' },
      { itemType: 'BLOCK', localId: 'b-1', block: { name: 'Destino', items: [] } },
    ] }} />);
    await user.click(screen.getByRole('button', { name: 'Opciones de pregunta' }));
    await user.click(screen.getByRole('button', { name: 'Mover a Destino' }));
    expect(screen.queryByRole('button', { name: 'Opciones de pregunta' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Sacar del bloque' }));
    expect(screen.getByRole('button', { name: 'Opciones de pregunta' })).toBeVisible();
  });

  it('edita contenido de texto alojado dentro de un bloque', async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ title: 'Plantilla', items: [
      { itemType: 'BLOCK', localId: 'b-1', block: { name: 'Introducción', items: [{ itemType: 'TEXT', textBody: 'Original' }] } },
    ] }} />);
    await user.click(screen.getByRole('button', { name: 'Opciones del texto informativo del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Editar texto' }));
    const dialog = screen.getByRole('dialog', { name: 'EDITAR TEXTO' });
    const textbox = within(dialog).getByRole('textbox', { name: /Contenido/ });
    await user.clear(textbox);
    await user.type(textbox, 'Revisado');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(screen.getByText('Revisado')).toBeVisible();
  });

  it('edita un bloque local y conserva sus preguntas', async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ title: 'Plantilla', items: [{ itemType: 'BLOCK', localId: 'b-1', block: {
      name: 'Antes', items: [{ itemType: 'QUESTION', localId: 'q-1', questionType: 'TEXT', statement: 'Pregunta del bloque' }],
    } }] }} />);
    await user.click(screen.getByRole('button', { name: 'Opciones de bloque' }));
    await user.click(screen.getByRole('button', { name: 'Editar bloque' }));
    const dialog = screen.getByRole('dialog', { name: 'EDITAR BLOQUE' });
    const name = within(dialog).getByRole('textbox', { name: /Nombre del bloque/ });
    await user.clear(name);
    await user.type(name, 'Después');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar bloque' }));
    expect(screen.getByRole('heading', { name: /Después/ })).toBeVisible();
    expect(screen.getByRole('heading', { name: /Pregunta del bloque/ })).toBeVisible();
  });

  it('inserta preguntas y bloques del catálogo en la posición de destino', () => {
    const onDropQuestion = vi.fn();
    const onDropBlock = vi.fn();
    const { container } = render(<MemoryRouter><TemplateBuilder
      data={{ title: 'Plantilla', items: [{ itemType: 'TEXT', textBody: 'Inicio' }] }}
      onDropQuestionFromSidebar={onDropQuestion}
      onDropBlockFromSidebar={onDropBlock}
    /></MemoryRouter>);
    const dropzone = container.querySelector('.block-empty-dropzone');
    fireEvent.dragOver(dropzone, { dataTransfer: { effectAllowed: 'copy', dropEffect: '' } });
    expect(dropzone).toHaveClass('drop-active');
    fireEvent.drop(dropzone, { dataTransfer: { getData: () => JSON.stringify({ type: 'CATALOG_QUESTION', question: { id: 4 } }) } });
    expect(onDropQuestion).toHaveBeenCalledWith({ id: 4 }, 1);
    fireEvent.drop(dropzone, { dataTransfer: { getData: () => JSON.stringify({ type: 'CATALOG_BLOCK', block: { id: 8 } }) } });
    expect(onDropBlock).toHaveBeenCalledWith({ id: 8 }, 1);
  });

  it('ordena elementos arrastrados y saca un elemento de un bloque', () => {
    const { container } = render(<Harness initial={{ title: 'Plantilla', items: [
      { itemType: 'BLOCK', localId: 'b-1', block: { name: 'Inicio', items: [{ itemType: 'TEXT', textBody: 'Texto anidado' }] } },
      { itemType: 'TEXT', textBody: 'Texto final' },
    ] }} />);
    const dropzone = container.querySelector('.block-empty-dropzone');
    fireEvent.drop(dropzone, { dataTransfer: { getData: () => JSON.stringify({ type: 'BLOCK_INTERNAL_SORT', blockIndex: 0, index: 0 }) } });
    expect(screen.getByText('Texto anidado')).toBeVisible();
    expect(container.querySelectorAll('.block-question-item')).toHaveLength(3);
    fireEvent.drop(dropzone, { dataTransfer: { getData: () => JSON.stringify({ type: 'INTERNAL_SORT', index: 1 }) } });
    expect(screen.getByText('Texto final')).toBeVisible();
  });
});
