import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from './apiClient.js';
import {
  deleteNotification,
  getNotifications,
  getUnreadNotificationsCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from './notificationsApi.js';

vi.mock('./apiClient.js', () => ({ apiClient: vi.fn() }));

describe('notificationsApi', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    apiClient.mockResolvedValue({});
  });

  it('transmite paginación y filtros al listar notificaciones', () => {
    getNotifications({ page: 2, size: 10, unreadOnly: true, type: 'COLLABORATION' });
    const [path] = apiClient.mock.lastCall;
    const url = new URL(path, 'https://example.test');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: '2', size: '10', unreadOnly: 'true', type: 'COLLABORATION',
    });
  });

  it.each([
    [{ count: 4 }, 4],
    [{ unreadCount: 3 }, 3],
    [2, 2],
    [{}, 0],
  ])('normaliza la respuesta del contador %j', async (response, expected) => {
    apiClient.mockResolvedValue(response);
    await expect(getUnreadNotificationsCount()).resolves.toBe(expected);
  });

  it('avisa al resto de la aplicación cuando cambia una notificación', async () => {
    const listener = vi.fn();
    window.addEventListener('notifications-changed', listener);
    try {
      await markNotificationAsRead(7);
      await markAllNotificationsAsRead();
      await deleteNotification(7);
      expect(listener).toHaveBeenCalledTimes(3);
      expect(apiClient).toHaveBeenNthCalledWith(1, '/notifications/7/read', { method: 'PATCH' });
      expect(apiClient).toHaveBeenNthCalledWith(2, '/notifications/read-all', { method: 'PATCH' });
      expect(apiClient).toHaveBeenNthCalledWith(3, '/notifications/7', { method: 'DELETE' });
    } finally {
      window.removeEventListener('notifications-changed', listener);
    }
  });

  it('no emite un cambio si el servidor rechaza la operación', async () => {
    const listener = vi.fn();
    apiClient.mockRejectedValue(new Error('No autorizado'));
    window.addEventListener('notifications-changed', listener);
    try {
      await expect(markNotificationAsRead(7)).rejects.toThrow('No autorizado');
      expect(listener).not.toHaveBeenCalled();
    } finally {
      window.removeEventListener('notifications-changed', listener);
    }
  });
});
