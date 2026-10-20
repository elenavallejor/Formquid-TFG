import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { NotificationsModal } from './NotificationsModal.jsx';
import {
  deleteNotification, getNotificationDestination, getNotifications,
  markAllNotificationsAsRead, markNotificationAsRead,
} from '../../api/notificationsApi.js';

vi.mock('../../api/notificationsApi.js', () => ({
  deleteNotification: vi.fn(),
  getNotificationDestination: vi.fn(),
  getNotifications: vi.fn(),
  markAllNotificationsAsRead: vi.fn(),
  markNotificationAsRead: vi.fn(),
}));

const unread = { id: 1, title: 'Invitación', message: 'Colabora en la encuesta', createdAt: '2026-10-19T10:00:00Z', targetType: 'SURVEY', channel: 'EMAIL', readAt: null };
const read = { id: 2, title: 'Aviso leído', message: 'Todo listo', createdAt: '2026-10-19T09:00:00Z', targetType: 'USER', readAt: '2026-10-19T09:30:00Z' };

function renderModal() {
  const onClose = vi.fn();
  render(<MemoryRouter initialEntries={['/']}>
    <NotificationsModal onClose={onClose} />
    <Routes><Route path="/" element={null} /><Route path="/encuesta" element={<p>Destino de encuesta</p>} /></Routes>
  </MemoryRouter>);
  return onClose;
}

describe('NotificationsModal', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getNotifications.mockResolvedValue({ content: [unread, read], totalElements: 2, totalPages: 1 });
    markNotificationAsRead.mockResolvedValue({ ...unread, readAt: '2026-10-19T11:00:00Z' });
  });

  it('carga el listado y marca como leída una notificación', async () => {
    const user = userEvent.setup();
    renderModal();
    expect(await screen.findByRole('heading', { name: 'Invitación' })).toBeVisible();
    expect(getNotifications).toHaveBeenCalledWith(expect.objectContaining({ page: 0, unreadOnly: true }));
    await user.click(screen.getByRole('button', { name: 'Marcar como leída' }));
    expect(markNotificationAsRead).toHaveBeenCalledWith(1);
    expect(screen.queryByRole('heading', { name: 'Invitación' })).not.toBeInTheDocument();
  });

  it('permite consultar todas y eliminar una ya leída', async () => {
    const user = userEvent.setup();
    deleteNotification.mockResolvedValue(undefined);
    renderModal();
    await screen.findByRole('heading', { name: 'Aviso leído' });
    await user.click(screen.getByRole('button', { name: 'Todas' }));
    expect(getNotifications).toHaveBeenLastCalledWith(expect.objectContaining({ unreadOnly: false }));
    await user.click(screen.getByRole('button', { name: 'Eliminar definitivamente: Aviso leído' }));
    expect(deleteNotification).toHaveBeenCalledWith(2);
    expect(screen.queryByRole('heading', { name: 'Aviso leído' })).not.toBeInTheDocument();
  });

  it('marca todas y navega al destino de una notificación', async () => {
    const user = userEvent.setup();
    markAllNotificationsAsRead.mockResolvedValue(undefined);
    getNotificationDestination.mockResolvedValue({ url: '/encuesta' });
    const onClose = renderModal();
    await screen.findByRole('heading', { name: 'Invitación' });
    await user.click(screen.getByRole('button', { name: 'Marcar todas como leídas' }));
    expect(markAllNotificationsAsRead).toHaveBeenCalledOnce();
    await user.click(await screen.findByRole('button', { name: 'Ir a la encuesta' }));
    await waitFor(() => expect(getNotificationDestination).toHaveBeenCalledWith(1));
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(await screen.findByText('Destino de encuesta')).toBeVisible();
  });

  it('muestra el error cuando falla la carga', async () => {
    getNotifications.mockRejectedValue(new Error('Servicio no disponible'));
    const onClose = renderModal();
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Cerrar notificaciones' }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
