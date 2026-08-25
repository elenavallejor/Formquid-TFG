import { apiClient } from './apiClient.js';

/**
 * Gestiona la carga agregada de los datos necesarios para construir el panel administrativo.
 */
export function getAdminDashboard() {
  return apiClient('/admin/dashboard', { method: 'GET' });
}
