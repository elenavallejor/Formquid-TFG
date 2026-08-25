import { apiClient } from './apiClient.js';

/**
 * Gestiona la consulta y actualización de las notificaciones del usuario autenticado.
 */
export function getNotifications() {
  return apiClient('/notifications', { method: 'GET' });
}

export async function getUnreadNotificationsCount() {
  const data = await apiClient('/notifications/unread-count', {
    method: 'GET',
  });

  return data?.count ??
    data?.unreadCount ??
    (typeof data === 'number' ? data : 0);
}

export function markNotificationAsRead(notificationId) {
  return apiClient(`/notifications/${notificationId}/read`, {
    method: 'PATCH',
  });
}

export function deleteNotification(notificationId) {
  return apiClient(`/notifications/${notificationId}`, {
    method: 'DELETE',
  });
}
