import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Filter } from './Filter.jsx';

const options = [{ value: 'TITLE', label: 'Título' }, { value: 'OWNER', label: 'Autor' }];
const choiceFilter = { label: 'Estado', value: 'ALL', resetValue: 'ALL', options: [{ value: 'ALL', label: 'Todos' }, { value: 'OPEN', label: 'Abiertas' }] };

describe('Filter', () => {
  it('aplica texto, opciones y estado y permite restablecerlos', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const onReset = vi.fn();
    render(<Filter label="Filtrar" options={options} selectedValues={['TITLE', 'OWNER']} textFilter={{ label: 'Etiqueta', value: '' }} choiceFilter={choiceFilter} onChange={onChange} onReset={onReset} />);
    const trigger = screen.getByRole('button', { name: /^Filtrar:/ });
    expect(trigger).not.toHaveAttribute('aria-controls');
    await user.click(trigger);
    expect(document.getElementById(trigger.getAttribute('aria-controls')))
      .toBe(screen.getByRole('group', { name: 'Filtrar' }));
    const menu = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(menu).getByRole('checkbox', { name: 'Autor' }));
    await user.type(within(menu).getByRole('searchbox', { name: 'Etiqueta' }), ' salud ');
    await user.click(within(menu).getByRole('radio', { name: 'Abiertas' }));
    await user.click(within(menu).getByRole('button', { name: 'Aplicar' }));
    expect(onChange).toHaveBeenCalledWith(['TITLE'], 'salud', 'OPEN');
    expect(trigger).not.toHaveAttribute('aria-controls');
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(screen.getByRole('button', { name: 'Borrar filtros' }));
    expect(onChange).toHaveBeenLastCalledWith(['TITLE', 'OWNER'], '', 'ALL');
    expect(onReset).toHaveBeenCalledOnce();
  });

  it('abre el diálogo móvil y lo cierra sin aplicar cambios', async () => {
    vi.spyOn(window, 'matchMedia').mockImplementation((query) => ({ matches: true, media: query, addEventListener: () => {}, removeEventListener: () => {} }));
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Filter label="Filtros" options={options} selectedValues={['TITLE']} onChange={onChange} />);
    await user.click(screen.getByRole('button', { name: /^Filtros:/ }));
    expect(screen.getByRole('dialog', { name: 'Filtros' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar filtros' }));
    expect(screen.queryByRole('dialog', { name: 'Filtros' })).not.toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('permite mostrar una selección exclusiva como select', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Filter
      label="Filtrar"
      choiceFilter={{
        label: 'Categoría',
        control: 'select',
        value: '',
        resetValue: '',
        options: [{ value: '', label: 'Todas' }, { value: 'ORDENADORES', label: 'Ordenadores' }],
      }}
      onChange={onChange}
    />);

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.selectOptions(within(filters).getByRole('combobox', { name: 'Categoría' }), 'ORDENADORES');
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));
    expect(onChange).toHaveBeenCalledWith([], '', 'ORDENADORES');
  });

  it('permite seleccionar varias opciones en un selector desplegable', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Filter
      label="Filtrar"
      choiceFilter={{
        label: 'Categoría',
        control: 'multiselect',
        value: [],
        resetValue: [],
        options: [{ value: 'ORDENADORES', label: 'Ordenadores' }, { value: 'SALUD', label: 'Salud' }],
      }}
      onChange={onChange}
    />);

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filters).getByLabelText('Seleccionar categoría'));
    await user.click(within(filters).getByRole('checkbox', { name: 'Ordenadores' }));
    await user.click(within(filters).getByRole('checkbox', { name: 'Salud' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));
    expect(onChange).toHaveBeenLastCalledWith([], '', ['ORDENADORES', 'SALUD']);

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(within(screen.getByRole('group', { name: 'Filtrar' })).getByRole('button', { name: 'Borrar filtros' }));
    expect(onChange).toHaveBeenLastCalledWith([], '', []);
  });
});
