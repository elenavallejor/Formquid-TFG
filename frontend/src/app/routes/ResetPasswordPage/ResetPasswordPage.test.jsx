import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { confirmPasswordReset } from '../../../api/authApi.js';
import { ResetPasswordPage } from './ResetPasswordPage.jsx';

vi.mock('../../../api/authApi.js', () => ({ confirmPasswordReset: vi.fn() }));

const token = 'a'.repeat(43);

function renderReset(path = `/restablecer-contrasena?token=${token}`) {
  render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
  </Routes></MemoryRouter>);
}

describe('ResetPasswordPage', () => {
  beforeEach(() => vi.resetAllMocks());

  it('requiere una contraseña válida y su confirmación', async () => {
    const user = userEvent.setup();
    renderReset();
    await user.type(screen.getByLabelText('Nueva contraseña'), 'abc');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'abcd');
    await user.click(screen.getByRole('button', { name: 'Guardar nueva contraseña' }));
    expect(screen.getByText('La contraseña debe tener entre 8 y 120 caracteres.')).toBeVisible();
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeVisible();
    expect(confirmPasswordReset).not.toHaveBeenCalled();
  });

  it('usa el token del enlace y confirma el cambio', async () => {
    const user = userEvent.setup();
    confirmPasswordReset.mockResolvedValue(null);
    renderReset();
    await user.type(screen.getByLabelText('Nueva contraseña'), 'new-secret-123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'new-secret-123');
    await user.click(screen.getByRole('button', { name: 'Guardar nueva contraseña' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Tu contraseña se ha actualizado');
    expect(confirmPasswordReset).toHaveBeenCalledWith(token, 'new-secret-123');
  });

  it('solicita un enlace nuevo cuando falta el token', () => {
    renderReset('/restablecer-contrasena');
    expect(screen.getByRole('status')).toHaveTextContent('Este enlace no es válido');
    expect(confirmPasswordReset).not.toHaveBeenCalled();
  });
});
