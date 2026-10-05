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

const ROLE_LABELS = {
  EDITOR: 'EDITOR',
  ADMIN: 'ADMINISTRADOR/A',
  SUPERADMIN: 'SUPERADMINISTRADOR/A',
};

const GENDER_LABELS = {
  MALE: 'Hombre',
  FEMALE: 'Mujer',
  OTHER: 'Otro',
};

export function getUserRoleLabel(role) {
  return ROLE_LABELS[role] ?? role ?? 'Sin rol';
}

export function getUserGenderLabel(gender) {
  return GENDER_LABELS[gender] ?? 'No especificado';
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

export function filterUsers(users, searchQuery) {
  const query = searchQuery.trim().toLowerCase();

  if (!query) return users;

  return users.filter((user) => {
    const name = String(user.name ?? '').toLowerCase();
    const email = String(user.email ?? '').toLowerCase();
    const institution = String(user.institution ?? '').toLowerCase();
    const gender = getUserGenderLabel(user.gender).toLowerCase();
    const role = getUserRoleLabel(user.role).toLowerCase();

    return (
      name.includes(query) ||
      email.includes(query) ||
      institution.includes(query) ||
      gender.includes(query) ||
      role.includes(query)
    );
  });
}

export function sortUsers(users, sortOrder) {
  return [...users].sort((a, b) => {
    if (sortOrder === 'NAME_ASC') {
      return String(a.name ?? '').localeCompare(String(b.name ?? ''), 'es', { sensitivity: 'base' });
    }

    if (sortOrder === 'NAME_DESC') {
      return String(b.name ?? '').localeCompare(String(a.name ?? ''), 'es', { sensitivity: 'base' });
    }

    const dateA = new Date(a.createdAt ?? 0).getTime();
    const dateB = new Date(b.createdAt ?? 0).getTime();

    return sortOrder === 'OLDEST' ? dateA - dateB : dateB - dateA;
  });
}

export function getVisibleUsers(users, searchQuery, sortOrder) {
  return sortUsers(filterUsers(users, searchQuery), sortOrder);
}
