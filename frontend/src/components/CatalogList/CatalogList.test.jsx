import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MdInventory } from 'react-icons/md';
import { describe, expect, it, vi } from 'vitest';

import { CatalogList } from './CatalogList.jsx';

const config = {
  icon: MdInventory, label: 'Bloques', singularLabel: 'Bloque', titleKey: 'name',
  descriptionKey: 'description', detailKey: 'questionCount', detailLabel: 'Preguntas',
};
const item = { id: 2, name: 'Estudio', description: 'Contexto', questionCount: 3, creatorName: 'Ana' };

function renderList(props = {}) {
  return render(<CatalogList items={[item]} config={config} itemsPerPage={10} {...props} />);
}

describe('CatalogList', () => {
  it('distingue carga, error y ausencia de resultados', () => {
    const view = renderList({ items: [], isLoading: true });
    expect(screen.queryByText('No se encontraron bloques disponibles.')).not.toBeInTheDocument();
    view.rerender(<CatalogList items={[]} config={config} errorMessage="Catálogo no disponible" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Catálogo no disponible');
    view.rerender(<CatalogList items={[]} config={config} />);
    expect(screen.getByText('No se encontraron bloques disponibles.')).toBeVisible();
  });

  it('muestra un listado desplazable y permite consultar un elemento', async () => {
    const user = userEvent.setup();
    const onView = vi.fn();
    renderList({ items: Array.from({ length: 6 }, (_, index) => ({ ...item, id: index, name: `Bloque ${index}` })), onView });
    expect(screen.getByRole('region', { name: 'Bloques: resultados desplazables' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver detalles de Bloque 0' }));
    expect(onView).toHaveBeenCalledWith(expect.objectContaining({ id: 0 }));
  });

  it('permite recorrer y activar las acciones con teclado', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderList({ canManageCatalog: true, onView: vi.fn(), onEdit, onDelete });
    const trigger = screen.getByRole('button', { name: 'Opciones de Estudio' });
    trigger.focus();
    fireEvent.keyDown(trigger, { key: 'ArrowDown' });
    const menu = screen.getByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: 'Ver detalles' })).toHaveFocus();
    fireEvent.keyDown(menu, { key: 'End' });
    expect(within(menu).getByRole('menuitem', { name: 'Borrar' })).toHaveFocus();
    fireEvent.keyDown(menu, { key: 'Home' });
    expect(within(menu).getByRole('menuitem', { name: 'Ver detalles' })).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();

    await user.click(trigger);
    await user.click(screen.getByRole('menuitem', { name: 'Editar' }));
    expect(onEdit).toHaveBeenCalledWith(2);
    await user.click(trigger);
    await user.click(screen.getByRole('menuitem', { name: 'Borrar' }));
    expect(onDelete).toHaveBeenCalledWith(item);
  });
});


it('mantiene la paleta original de los iconos de cada tipo de pregunta', () => {
  const types = [
    ['TEXT', 'Texto', '#6539B1'], ['NUMBER', 'Numérica', '#DE3ECE'],
    ['SINGLE_CHOICE', 'Opción única', '#39B1AB'], ['MULTIPLE_CHOICE', 'Opción múltiple', '#FFBD30'],
    ['SCALE', 'Escala', '#395FB1'], ['MATRIX_SCALE', 'Matriz de escala', '#5E9800'],
    ['BOOLEAN', 'Dos opciones', '#FB963C'],
  ];
  renderList({
    config: { ...config, titleKey: 'statement', detailKey: 'questionType' },
    items: types.map(([questionType], id) => ({ id, questionType, statement: `Pregunta ${id}` })),
  });
  for (const [, label, color] of types) expect(screen.getByTitle(label)).toHaveStyle({ color });
});

it('identifica una categoría sin inventar una categoría por defecto si faltan sus metadatos', () => {
  renderList({ items: [{ ...item, category: 'REALIDAD_AUMENTADA' }, { ...item, id: 3, category: null }] });
  expect(screen.getByText('Realidad aumentada')).toBeVisible();
  expect(screen.getByText('Sin categoría')).toBeVisible();
});


it('muestra el icono configurado para la categoría junto a su nombre', () => {
  const { container } = renderList({
    items: [{ ...item, category: 'ORDENADORES' }],
    categories: [{ code: 'ORDENADORES', name: 'Ordenadores', icon: 'BsLaptop' }],
  });
  const category = container.querySelector('.catalog-item-category');
  expect(category).toHaveTextContent(/^Ordenadores$/);
  expect(category.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
});
