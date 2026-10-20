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
