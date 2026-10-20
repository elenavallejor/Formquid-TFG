import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser, login, logout } from '../api/authApi.js';
import { ApiError } from '../api/apiClient.js';
import { AuthProvider } from './AuthContext.jsx';
import { useAuth } from './useAuth.js';

vi.mock('../api/authApi.js', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));

const editor = { id: 7, email: 'editor@example.com', role: 'EDITOR' };

function SessionState() {
  const auth = useAuth();

  return (
    <div>
      <p role="status">{auth.isInitializing ? 'Iniciando' : auth.authStatus}</p>
      {auth.user && <p>{auth.user.email}</p>}
      {auth.authError && <p role="alert">{auth.authError}</p>}
      <button type="button" onClick={() => void auth.login({ email: editor.email, password: 'password' }).catch(() => {})}>Entrar</button>
      <button type="button" onClick={() => void auth.logout().catch(() => {})}>Salir</button>
    </div>
  );
}

function renderSession() {
  render(<AuthProvider><SessionState /></AuthProvider>);
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCurrentUser.mockResolvedValue(null);
  });

  it('restaura una sesión válida', async () => {
    getCurrentUser.mockResolvedValue({ user: editor });
    renderSession();
    expect(await screen.findByText(editor.email)).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('authenticated');
  });

  it('trata un 401 como sesión anónima', async () => {
    getCurrentUser.mockRejectedValue(new ApiError('Sin sesión', 401));
    renderSession();
    expect(await screen.findByRole('status')).toHaveTextContent('anonymous');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('expone el error cuando falla la comprobación de sesión', async () => {
    getCurrentUser.mockRejectedValue(new Error('Red no disponible'));
    renderSession();
    expect(await screen.findByRole('alert')).toHaveTextContent('Red no disponible');
    expect(screen.getByRole('status')).toHaveTextContent('error');
  });

  it('inicia y cierra la sesión después de restaurar el estado anónimo', async () => {
    const user = userEvent.setup();
    login.mockResolvedValue({ user: editor });
    logout.mockResolvedValue(null);
    renderSession();
    await screen.findByText('anonymous');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByText(editor.email)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Salir' }));
    expect(await screen.findByRole('status')).toHaveTextContent('anonymous');
    expect(screen.queryByText(editor.email)).not.toBeInTheDocument();
  });

  it('rechaza una respuesta de login que no identifica al usuario', async () => {
    const user = userEvent.setup();
    login.mockResolvedValue({ user: { id: 7 } });
    renderSession();
    await screen.findByText('anonymous');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('no ha devuelto los datos del usuario');
    expect(screen.getByRole('status')).toHaveTextContent('anonymous');
  });
});
