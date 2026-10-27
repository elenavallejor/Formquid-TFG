import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Header } from './Header.jsx';
import { AuthContext } from '../../context/authContext.js';
import { getUnreadNotificationsCount } from '../../api/notificationsApi.js';

vi.mock('../../api/notificationsApi.js', () => ({ getUnreadNotificationsCount: vi.fn() }));
vi.mock('../NotificationsModal/NotificationsModal.jsx', () => ({
  NotificationsModal: ({ onClose }) => <button type="button" onClick={onClose}>Cerrar notificaciones</button>,
}));

function renderHeader(auth, route = '/') {
  return render(<AuthContext.Provider value={auth}>
    <MemoryRouter initialEntries={[route]}>
      <Header />
      <Routes><Route path="/" element={<p>Inicio</p>} /><Route path="/panel" element={<p>Panel</p>} /></Routes>
    </MemoryRouter>
  </AuthContext.Provider>);
}

describe('Header', () => {
  beforeEach(() => vi.resetAllMocks());

  it('presenta los accesos de inicio a quienes no han iniciado sesión', () => {
    renderHeader({ isAuthenticated: false, user: null });
    expect(screen.getByRole('navigation', { name: 'Navegación principal' }))
      .toContainElement(screen.getByRole('link', { name: 'FormQuid - Inicio' }));
    expect(screen.getByRole('link', { name: 'Inicia sesión' })).toHaveAttribute('href', '/inicia-sesion');
    expect(screen.getByRole('link', { name: 'Solicita tu cuenta' })).toHaveAttribute('href', '/solicita-cuenta');
    expect(screen.queryByRole('button', { name: /Ver notificaciones/ })).not.toBeInTheDocument();
  });

  it('refresca el contador y permite abrir y cerrar notificaciones', async () => {
    const user = userEvent.setup();
    getUnreadNotificationsCount.mockResolvedValue(3);
    renderHeader({ isAuthenticated: true, user: { id: 7, fullName: 'Ana García', role: 'EDITOR' }, logout: vi.fn() });
    const button = await screen.findByRole('button', { name: 'Ver notificaciones. 3 sin leer.' });
    expect(screen.getByText('Ana García')).toBeVisible();
    expect(screen.getByText('Editor')).toBeVisible();
    await user.click(button);
    expect(screen.getByRole('button', { name: 'Cerrar notificaciones' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar notificaciones' }));
    expect(screen.queryByRole('button', { name: 'Cerrar notificaciones' })).not.toBeInTheDocument();
    window.dispatchEvent(new Event('notifications-changed'));
    expect(getUnreadNotificationsCount).toHaveBeenCalledTimes(2);
  });

  it('muestra la foto guardada y recupera las iniciales si falla la imagen', () => {
    getUnreadNotificationsCount.mockResolvedValue(0);
    const { container } = renderHeader({ isAuthenticated: true, user: {
      id: 7, name: 'Ana García', role: 'EDITOR', profileImageKey: '/api/v1/users/7/photo/imagen.png',
    }, logout: vi.fn() });
    const image = container.querySelector('.user-avatar img');
    expect(image).toHaveAttribute('src', 'http://localhost:8080/api/v1/users/7/photo/imagen.png');

    fireEvent.error(image);

    expect(container.querySelector('.user-avatar')).toHaveTextContent('AG');
  });

  it('cierra la sesión y lleva a inicio', async () => {
    const user = userEvent.setup();
    const logout = vi.fn().mockResolvedValue(undefined);
    getUnreadNotificationsCount.mockResolvedValue(0);
    renderHeader({ isAuthenticated: true, user: { id: 7, email: 'ana@example.com', role: 'ADMIN' }, logout }, '/panel');
    expect(screen.getByText('Administrador')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    expect(logout).toHaveBeenCalledOnce();
    expect(screen.getByText('Inicio')).toBeVisible();
  });
});
