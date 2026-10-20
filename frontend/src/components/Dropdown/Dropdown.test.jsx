import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Dropdown } from './Dropdown.jsx';

function renderDropdown(props = {}) {
  return render(<Dropdown aria-label="Ordenar" name="sort" value="a" {...props}>
    <option value="a">Primero</option>
    <option value="b">Segundo</option>
    <option value="c">Tercero</option>
  </Dropdown>);
}

describe('Dropdown', () => {
  it('expone el valor del formulario y comunica la selección', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const { container } = renderDropdown({ onChange });
    expect(container.querySelector('input[name="sort"]')).toHaveValue('a');
    await user.click(screen.getByRole('button', { name: 'Ordenar: Primero' }));
    fireEvent.click(within(screen.getByRole('group', { name: 'Ordenar' })).getByRole('button', { name: 'Segundo' }));
    expect(onChange).toHaveBeenCalledWith({ target: { name: 'sort', value: 'b' } });
    expect(screen.queryByRole('group', { name: 'Ordenar' })).not.toBeInTheDocument();
  });

  it('permite recorrer opciones con teclado y cerrar con Escape', async () => {
    const user = userEvent.setup();
    renderDropdown();
    const trigger = screen.getByRole('button', { name: 'Ordenar: Primero' });
    trigger.focus();
    await user.keyboard('{ArrowDown}');
    const menu = screen.getByRole('group', { name: 'Ordenar' });
    const first = within(menu).getByRole('button', { name: 'Primero' });
    expect(first).toHaveFocus();
    await user.keyboard('{End}');
    expect(within(menu).getByRole('button', { name: 'Tercero' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('group', { name: 'Ordenar' })).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('impide abrir las opciones cuando está deshabilitado', async () => {
    const user = userEvent.setup();
    renderDropdown({ disabled: true });
    const trigger = screen.getByRole('button', { name: 'Ordenar: Primero' });
    expect(trigger).toBeDisabled();
    await user.click(trigger);
    expect(screen.queryByRole('group', { name: 'Ordenar' })).not.toBeInTheDocument();
  });
});
