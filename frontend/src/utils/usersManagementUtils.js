/**
 * Agrupación de funciones auxiliares para la pantalla de gestión de usuarios.
 */
export const DEFAULT_USERS_PAGE_SIZE = 10;

export const USERS_PAGE_SIZE_OPTIONS = [5, 10, 20];

export const USER_SORT_OPTIONS = [
  { value: 'NAME_ASC', label: 'Nombre A-Z' },
  { value: 'NAME_DESC', label: 'Nombre Z-A' },
  { value: 'NEWEST', label: 'Más reciente' },
  { value: 'OLDEST', label: 'Más antigua' },
];

export const USER_SEARCH_OPTIONS = [
  { value: 'NAME', label: 'Nombre' },
  { value: 'EMAIL', label: 'Correo' },
  { value: 'INSTITUTION', label: 'Institución' },
  { value: 'COUNTRY', label: 'País' },
  { value: 'ROLE', label: 'Rol' },
];

const ROLE_LABELS = {
  EDITOR: 'EDITOR',
  ADMIN: 'ADMINISTRADOR/A',
  SUPERADMIN: 'SUPERADMINISTRADOR/A',
};

export function getUserRoleLabel(role) {
  return ROLE_LABELS[role] ?? role ?? 'Sin rol';
}

export function getVisiblePages(currentPage, totalPages) {
  if (totalPages <= 0) return [];
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index);

  const firstPage = Math.min(Math.max(currentPage - 2, 0), totalPages - 5);
  return Array.from({ length: 5 }, (_, index) => firstPage + index);
}

export function getPaginationRange(currentPage, pageSize, totalItems) {
  if (totalItems <= 0) {
    return { startItem: 0, endItem: 0 };
  }

  return {
    startItem: currentPage * pageSize + 1,
    endItem: Math.min((currentPage + 1) * pageSize, totalItems),
  };
}

export function isCurrentUser(user, currentUser) {
  return Boolean(user?.id && currentUser?.id && user.id === currentUser.id);
}

export function getNextRole(role) {
  if (role === 'EDITOR') return 'ADMIN';
  if (role === 'ADMIN') return 'EDITOR';
  return null;
}

export function canChangeUserRole(user, currentUser) {
  return !isCurrentUser(user, currentUser) && user?.role !== 'SUPERADMIN';
}

export function canDeleteUser(user, currentUser) {
  return !isCurrentUser(user, currentUser) && user?.role !== 'SUPERADMIN';
}
