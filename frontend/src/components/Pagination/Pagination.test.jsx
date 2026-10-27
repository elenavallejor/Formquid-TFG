import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Pagination } from './Pagination.jsx';

describe('Pagination', () => {
  it('cambia página y tamaño conservando el rango visible', async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    const onPageSizeChange = vi.fn();
    render(<Pagination currentPage={2} totalPages={8} totalItems={75} pageSize={10} pageSizeOptions={[10, 25]} onPageChange={onPageChange} onPageSizeChange={onPageSizeChange} />);
    expect(screen.getByText('Mostrando 21 a 30 de 75 elementos')).toBeVisible();
    expect(screen.getByRole('group', { name: 'Paginación' })).toBeVisible();
    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Página 3' })).toHaveAttribute('aria-current', 'page');
    await user.click(screen.getByRole('button', { name: 'Página siguiente' }));
    expect(onPageChange).toHaveBeenCalledWith(3);
    await user.click(screen.getByRole('button', { name: /Elementos por página:/ }));
    fireEvent.click(within(screen.getByRole('group', { name: 'Elementos por página' })).getByRole('button', { name: '25 por página' }));
    expect(onPageSizeChange).toHaveBeenCalledWith(25);
  });

  it('deshabilita avanzar en la última página del modo compacto', () => {
    render(<Pagination variant="compact" currentPage={1} totalPages={2} totalItems={15} onPageChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled();
  });
});
