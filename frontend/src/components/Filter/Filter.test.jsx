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
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const menu = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(menu).getByRole('checkbox', { name: 'Autor' }));
    await user.type(within(menu).getByRole('searchbox', { name: 'Etiqueta' }), ' salud ');
    await user.click(within(menu).getByRole('radio', { name: 'Abiertas' }));
    await user.click(within(menu).getByRole('button', { name: 'Aplicar' }));
    expect(onChange).toHaveBeenCalledWith(['TITLE'], 'salud', 'OPEN');
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
});
