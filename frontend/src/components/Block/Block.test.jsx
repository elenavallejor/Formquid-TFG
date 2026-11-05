import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Block } from './Block.jsx';

const block = {
  name: 'Hábitos',
  description: 'Rutinas diarias',
  contentItems: [
    { itemType: 'TEXT', textBody: 'Introduce tus respuestas' },
    { itemType: 'QUESTION', id: 3, questionType: 'TEXT', statement: '¿Cuántas horas duermes?' },
  ],
};

describe('Block', () => {
  it('muestra contenido y numeración en modo lectura', () => {
    render(<Block data={block} blockLabel="A" questionNumberStart={2} />);
    expect(screen.getByRole('heading', { name: /A\. Hábitos/ })).toBeVisible();
    expect(screen.getByText('Rutinas diarias')).toBeVisible();
    expect(screen.getByText('Introduce tus respuestas')).toBeVisible();
    expect(screen.getByRole('heading', { name: /Pregunta 3\.\s*¿Cuántas horas duermes\?/ })).toBeVisible();
    expect(screen.getByRole('textbox', { name: '¿Cuántas horas duermes?' })).toBeDisabled();
  });

  it('permite editar, borrar y mover elementos entre bloques', async () => {
    const user = userEvent.setup();
    const onMoveItem = vi.fn();
    const onEditItem = vi.fn();
    const onDeleteItem = vi.fn();
    const onMoveItemOut = vi.fn();
    const onMoveItemToTarget = vi.fn();
    const onRequiredChange = vi.fn();
    render(<Block
      data={block}
      blockIndex={0}
      onMoveItem={onMoveItem}
      onEditItem={onEditItem}
      onDeleteItem={onDeleteItem}
      onMoveItemOut={onMoveItemOut}
      onMoveItemToTarget={onMoveItemToTarget}
      onRequiredChange={onRequiredChange}
      moveTargets={[{ index: 1, label: 'Destino' }]}
    />);
    await user.click(screen.getByRole('button', { name: 'Mover elemento 2 del bloque hacia arriba' }));
    expect(onMoveItem).toHaveBeenCalledWith(1, 0);
    await user.click(screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Editar pregunta' }));
    expect(onEditItem).toHaveBeenCalledWith(1);
    await user.click(screen.getByRole('button', { name: 'Opciones del texto informativo del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar del bloque' }));
    expect(onDeleteItem).toHaveBeenCalledWith(0);
    await user.click(screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Sacar del bloque' }));
    expect(onMoveItemOut).toHaveBeenCalledWith(1);
    await user.click(screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' }));
    await user.click(screen.getByRole('button', { name: 'Mover a Destino' }));
    expect(onMoveItemToTarget).toHaveBeenCalledWith(1, 1);
  });

  it('acepta preguntas del catálogo y elementos movidos entre bloques', () => {
    const onDropQuestion = vi.fn();
    const onDropBlockItem = vi.fn();
    const onDragInside = vi.fn();
    const { container } = render(<Block data={block} blockIndex={2} onDropQuestion={onDropQuestion} onDropBlockItem={onDropBlockItem} onMoveItem={vi.fn()} onDragInside={onDragInside} />);
    const target = container.querySelector('.block-content-dropzone');
    const catalog = { types: ['application/x-formquid-question'], getData: () => JSON.stringify({ type: 'CATALOG_QUESTION', question: { id: 8 } }), dropEffect: '' };
    fireEvent.dragOver(target, { dataTransfer: catalog });
    expect(onDragInside).toHaveBeenCalled();
    expect(target).toHaveClass('drop-active');
    fireEvent.drop(target, { dataTransfer: catalog });
    expect(onDropQuestion).toHaveBeenCalledWith({ id: 8 }, 2);

    const moved = { types: ['application/x-formquid-block-item'], getData: () => JSON.stringify({ type: 'BLOCK_INTERNAL_SORT', blockIndex: 1, index: 0 }) };
    fireEvent.drop(target, { dataTransfer: moved });
    expect(onDropBlockItem).toHaveBeenCalledWith(1, 0, 2);
  });

  it('reordena dentro del bloque y admite una pregunta movida desde la plantilla', () => {
    const onMoveItem = vi.fn();
    const onDropTemplateQuestion = vi.fn();
    const { container } = render(<Block data={block} blockIndex={2} onMoveItem={onMoveItem} onDropTemplateQuestion={onDropTemplateQuestion} onDropQuestion={vi.fn()} />);
    const firstItem = container.querySelector('.block-content-item');
    fireEvent.keyDown(screen.getByRole('button', { name: 'Reordenar elemento 1 del bloque' }), { key: 'ArrowDown' });
    expect(onMoveItem).toHaveBeenCalledWith(0, 2);
    fireEvent.drop(firstItem, { dataTransfer: { getData: () => JSON.stringify({ type: 'BLOCK_INTERNAL_SORT', blockIndex: 2, index: 1 }) } });
    expect(onMoveItem).toHaveBeenCalledWith(1, 1);
    fireEvent.drop(firstItem, { dataTransfer: { getData: () => JSON.stringify({ type: 'INTERNAL_SORT', index: 3 }) } });
    expect(onDropTemplateQuestion).toHaveBeenCalledWith(3, 1);
  });

  it.each(['application/x-formquid-block-item', 'application/x-formquid-template-question'])('acepta arrastres internos de tipo %s', type => {
    const onMoveItem = vi.fn(), onDropTemplateQuestion = vi.fn();
    const { container } = render(<Block data={block} onDropQuestion={vi.fn()} onMoveItem={onMoveItem} onDropTemplateQuestion={onDropTemplateQuestion} />);
    const item = container.querySelector('.block-content-item');
    const end = container.querySelector('.block-content-dropzone');
    const dataTransfer = { types: [type], effectAllowed: 'move', dropEffect: '', getData: () => 'invalid' };
    fireEvent.dragOver(item, { dataTransfer });
    expect(dataTransfer.dropEffect).toBe('move');
    fireEvent.dragOver(end, { dataTransfer });
    expect(end).toHaveClass('drop-active');
    fireEvent.dragLeave(container.querySelector('.block-content'), { relatedTarget: document.body });
    expect(end).not.toHaveClass('drop-active');
    fireEvent.drop(item, { dataTransfer });
    expect(onMoveItem).not.toHaveBeenCalled();
    expect(onDropTemplateQuestion).not.toHaveBeenCalled();
  });
  it('ignora arrastres de tipos ajenos al constructor', () => {
    const { container } = render(<Block data={block} onMoveItem={vi.fn()} onDropQuestion={vi.fn()} onDropTemplateQuestion={vi.fn()} />);
    const end = container.querySelector('.block-content-dropzone');
    const dataTransfer = { types: ['text/plain'], dropEffect: 'none' };
    fireEvent.dragOver(container.querySelector('.block-content-item'), { dataTransfer });
    fireEvent.dragOver(end, { dataTransfer });
    expect(end).not.toHaveClass('drop-active');
    expect(dataTransfer.dropEffect).toBe('none');
  });
  it('usa el nombre y nivel de encabezado por defecto y gestiona teclado y menú', () => {
    const onMoveItem = vi.fn();
    render(<Block data={{ contentItems: block.contentItems }} headingLevel="incorrecto" onMoveItem={onMoveItem} onEditItem={vi.fn()} />);
    expect(screen.getByRole('heading', { name: 'Bloque sin nombre', level: 3 })).toBeVisible();
    const handle = screen.getByRole('button', { name: 'Reordenar elemento 2 del bloque' });
    fireEvent.keyDown(handle, { key: 'ArrowUp' });
    expect(onMoveItem).toHaveBeenCalledWith(1, 0);
    fireEvent.keyDown(handle, { key: 'Enter' });
    expect(onMoveItem).toHaveBeenCalledTimes(1);
    const menu = screen.getByRole('button', { name: 'Opciones de la pregunta 1 del bloque' });
    fireEvent.click(menu);
    expect(screen.getByRole('button', { name: 'Editar pregunta' })).toBeVisible();
    fireEvent.click(menu);
    expect(screen.queryByRole('button', { name: 'Editar pregunta' })).not.toBeInTheDocument();
  });
});
