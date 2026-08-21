import { apiClient } from './apiClient.js';

/**
 * Módulo de API para la gestión de notificaciones.
 */

/**
 * Obtiene la lista completa de notificaciones del usuario autenticado.
 * @returns {Promise<Array>}
 */
export async function getNotifications() {
  return apiClient('/notifications', {
    method: 'GET',
  });
}

/**
 * Obtiene la cantidad de notificaciones no leídas.
 * @returns {Promise<number>}
 */
export async function getUnreadNotificationsCount() {
  const data = await apiClient('/notifications/unread-count', {
    method: 'GET',
  });

  return data?.count ?? data?.unreadCount ?? (typeof data === 'number' ? data : 0);
}

/**
 * Marca una notificación como leída.
 * @param {string|number} notificationId
 */
export async function markNotificationAsRead(notificationId) {
  return apiClient(`/notifications/${notificationId}/read`, {
    method: 'PATCH',
  });
}

/**
 * Elimina manualmente una notificación.
 * @param {string|number} notificationId
 */
export async function deleteNotification(notificationId) {
  return apiClient(`/notifications/${notificationId}`, {
    method: 'DELETE',
  });
}