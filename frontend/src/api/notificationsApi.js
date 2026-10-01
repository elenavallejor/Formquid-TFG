import { apiClient } from './apiClient.js';

/**
 * Gestiona la consulta y actualización de las notificaciones del usuario autenticado.
 */
export function getNotifications({ page = 0, size = 5, unreadOnly = false, type = '' } = {}) {
  const query = new URLSearchParams({ page, size, unreadOnly });
  if (type) query.set('type', type);
  return apiClient(`/notifications?${query}`, { method: 'GET' });
}

export async function getUnreadNotificationsCount() {
  const data = await apiClient('/notifications/unread-count', {
    method: 'GET',
  });

  return data?.count ??
    data?.unreadCount ??
    (typeof data === 'number' ? data : 0);
}

async function mutateNotification(path, method) {
  const result = await apiClient(path, { method });
  window.dispatchEvent(new Event('notifications-changed'));
  return result;
}

export function markNotificationAsRead(notificationId) {
  return mutateNotification(`/notifications/${notificationId}/read`, 'PATCH');
}

export function markAllNotificationsAsRead() {
  return mutateNotification('/notifications/read-all', 'PATCH');
}

export function getNotificationDestination(notificationId) {
  return apiClient(`/notifications/${notificationId}/destination`, { method: 'GET' });
}

export function deleteNotification(notificationId) {
  return mutateNotification(`/notifications/${notificationId}`, 'DELETE');
}
