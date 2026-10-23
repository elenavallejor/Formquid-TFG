import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestPasswordReset } from '../../../api/authApi.js';
import { PasswordRecoveryPage } from './PasswordRecoveryPage.jsx';

vi.mock('../../../api/authApi.js', () => ({ requestPasswordReset: vi.fn() }));

describe('PasswordRecoveryPage', () => {
  beforeEach(() => vi.resetAllMocks());

  it('valida el correo antes de enviarlo', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><PasswordRecoveryPage /></MemoryRouter>);
    await user.click(screen.getByRole('button', { name: 'Enviar enlace de recuperación' }));
    expect(screen.getByText('Introduce tu correo electrónico.')).toBeVisible();
    expect(requestPasswordReset).not.toHaveBeenCalled();
  });

  it('confirma el envío sin revelar si existe la cuenta', async () => {
    const user = userEvent.setup();
    requestPasswordReset.mockResolvedValue(null);
    render(<MemoryRouter><PasswordRecoveryPage /></MemoryRouter>);
    await user.type(screen.getByRole('textbox', { name: 'Correo electrónico' }), 'ANA@Example.com');
    await user.click(screen.getByRole('button', { name: 'Enviar enlace de recuperación' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Si existe una cuenta');
    expect(requestPasswordReset).toHaveBeenCalledWith('ana@example.com');
  });
});
