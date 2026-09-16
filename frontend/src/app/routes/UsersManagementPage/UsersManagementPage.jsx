import { useEffect, useMemo, useRef, useState } from 'react';
import { MdChevronLeft, MdChevronRight, MdKeyboardArrowRight, MdMailOutline, MdOutlineBusiness, MdPersonOutline, MdSearch } from 'react-icons/md';

import { deleteUser, getUsers, updateUserRole } from '../../../api/usersApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';
import { DEFAULT_USERS_PAGE_SIZE, USER_SORT_OPTIONS, USERS_PAGE_SIZE_OPTIONS, canChangeUserRole, canDeleteUser, getNextRole, getUserGenderLabel, getUserRoleLabel, getVisiblePages, getVisibleUsers } from '../../../utils/usersManagementUtils.js';

import './UsersManagementPage.css';

/**
 * Muestra la pantalla de gestión de usuarios y permisos para superadministradores.
 */
export function UsersManagementPage() {
  const { user } = useAuth();
  const requestVersionRef = useRef(0);

  const [usersList, setUsersList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('NAME_ASC');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_USERS_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [roleModalState, setRoleModalState] = useState({ isOpen: false, targetUser: null, nextRole: null });
  const [deleteModalState, setDeleteModalState] = useState({ isOpen: false, targetUser: null });
  const [isProcessingAction, setIsProcessingAction] = useState(false);
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    const requestVersion = ++requestVersionRef.current;

    async function loadUsers() {
      try {
        setIsLoading(true);
        setErrorMessage('');
        setUsersList([]);

        const response = await getUsers(currentPage, itemsPerPage);

        if (requestVersion !== requestVersionRef.current) return;

        setUsersList(Array.isArray(response?.content) ? response.content : []);
        setTotalItems(Number(response?.totalElements ?? 0));
        setTotalPages(Number(response?.totalPages ?? 0));
      } catch (error) {
        if (requestVersion !== requestVersionRef.current) return;
        console.error('Error al obtener los usuarios:', error);
        setUsersList([]);
        setTotalItems(0);
        setTotalPages(0);
        setErrorMessage('No se han podido cargar los usuarios del sistema.');
      } finally {
        if (requestVersion === requestVersionRef.current) setIsLoading(false);
      }
    }

    void loadUsers();
    return undefined;
  }, [currentPage, itemsPerPage]);

  const visibleUsers = useMemo(
    () => getVisibleUsers(usersList, searchQuery, sortOrder),
    [usersList, searchQuery, sortOrder]
  );

  const visiblePages = useMemo(
    () => getVisiblePages(currentPage, totalPages),
    [currentPage, totalPages]
  );

  async function handleConfirmRoleChange() {
    const { targetUser, nextRole } = roleModalState;
    if (!targetUser || !nextRole) return;

    try {
      setIsProcessingAction(true);
      setActionError('');

      const updatedUser = await updateUserRole(targetUser.id, nextRole);

      setUsersList((prev) =>
        prev.map((item) =>
          item.id === targetUser.id ? { ...item, ...(updatedUser ?? {}), role: updatedUser?.role ?? nextRole } : item
        )
      );

      setRoleModalState({ isOpen: false, targetUser: null, nextRole: null });
    } catch (error) {
      setActionError(error.message || 'No se han podido actualizar los permisos del usuario.');
    } finally {
      setIsProcessingAction(false);
    }
  }

  async function handleConfirmDeleteUser() {
    const targetUser = deleteModalState.targetUser;
    if (!targetUser) return;

    try {
      setIsProcessingAction(true);
      setActionError('');

      await deleteUser(targetUser.id);

      setUsersList((prev) => prev.filter((item) => item.id !== targetUser.id));
      setTotalItems((prev) => Math.max(0, prev - 1));

      if (usersList.length === 1 && currentPage > 0) {
        setCurrentPage((prev) => Math.max(prev - 1, 0));
      }

      setDeleteModalState({ isOpen: false, targetUser: null });
    } catch (error) {
      setActionError(error.message || 'No se ha podido eliminar el usuario.');
    } finally {
      setIsProcessingAction(false);
    }
  }

  const startItem = totalItems === 0 ? 0 : currentPage * itemsPerPage + 1;
  const endItem = Math.min((currentPage + 1) * itemsPerPage, totalItems);
  const isPromotion = roleModalState.nextRole === 'ADMIN';
  const roleTargetName = roleModalState.targetUser?.name || 'este usuario';
  const deleteTargetName = deleteModalState.targetUser?.name || 'este usuario';

  return (
    <main className="users-page">
      <div className="users-container">
        <Breadcrumbs
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'GESTIÓN DE USUARIOS Y PERMISOS' },
          ]}
          separator={MdKeyboardArrowRight}
        />

        <header className="users-page-header">
          <h1>GESTIÓN DE USUARIOS Y PERMISOS</h1>
          <p className="users-subtitle">Gestiona a los usuarios de la plataforma y sus respectivos permisos.</p>
        </header>

        <section className="catalog-filters-bar" aria-label="Filtros de usuarios">
          <div className="search-input-wrapper">
            <MdSearch className="search-icon" aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre, correo, institución, género o rol..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="catalog-filters-right">
            <div className="sort-select-wrapper">
              <span className="sort-label">Ordenar por:</span>
              <Dropdown value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
                {USER_SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </Dropdown>
            </div>
          </div>
        </section>

        {isLoading ? (
          <div className="users-list-wrapper">
            <Skeleton variant="list" count={itemsPerPage} hasActions={true} />
          </div>
        ) : errorMessage ? (
          <div className="users-empty-state" role="alert">
            <p>{errorMessage}</p>
          </div>
        ) : visibleUsers.length === 0 ? (
          <div className="users-empty-state">
            <p>No se han encontrado usuarios que coincidan con la búsqueda.</p>
          </div>
        ) : (
          <div className="users-list-wrapper">
            {visibleUsers.map((item) => {
              const nextRole = getNextRole(item.role);
              const canUpdateRole = canChangeUserRole(item, user) && nextRole;
              const canRemoveUser = canDeleteUser(item, user);

              return (
                <article key={item.id} className="user-management-card">
                  <div className="user-card-info">
                    <h2 className="user-card-title">
                      {String(item.name ?? 'Usuario sin nombre').toUpperCase()} - {getUserRoleLabel(item.role)}
                    </h2>

                    <div className="user-card-details">
                      <span className="user-details-title">DATOS PERSONALES:</span>
                      <div className="user-details-badges">
                        <span className="user-detail-item">
                          <MdMailOutline aria-hidden="true" />
                          {item.email}
                        </span>
                        <span className="user-detail-item">
                          <MdOutlineBusiness aria-hidden="true" />
                          {item.institution || 'Sin institución'}
                        </span>
                        <span className="user-detail-item">
                          <MdPersonOutline aria-hidden="true" />
                          {getUserGenderLabel(item.gender)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="user-card-actions">
                    {canUpdateRole && (
                      <button
                        type="button"
                        className="btn-user-action btn-promote"
                        onClick={() => {
                          setActionError('');
                          setRoleModalState({ isOpen: true, targetUser: item, nextRole });
                        }}
                      >
                        {nextRole === 'ADMIN' ? 'ASCENDER A ADMINISTRADOR' : 'ELIMINAR PERMISOS'}
                      </button>
                    )}

                    {canRemoveUser && (
                      <button
                        type="button"
                        className="btn-user-action btn-delete-user"
                        onClick={() => {
                          setActionError('');
                          setDeleteModalState({ isOpen: true, targetUser: item });
                        }}
                      >
                        ELIMINAR CUENTA
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}

        {!isLoading && !errorMessage && (totalItems > 0 || totalPages > 0) && (
          <footer className="catalog-pagination-footer">
            <div className="pagination-count-label">
              Mostrando {startItem} a {endItem} de {totalItems} usuarios
            </div>

            <div className="pagination-numbers-bar">
              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage === 0 || isLoading}
                onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
              >
                <MdChevronLeft aria-hidden="true" />
              </button>

              {visiblePages.map((page) => (
                <button
                  key={page}
                  type="button"
                  className={`btn-page-number ${currentPage === page ? 'active' : ''}`}
                  disabled={isLoading}
                  onClick={() => setCurrentPage(page)}
                >
                  {page + 1}
                </button>
              ))}

              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages - 1))}
              >
                <MdChevronRight aria-hidden="true" />
              </button>
            </div>

            <div className="pagination-per-page">
              <Dropdown
                value={itemsPerPage}
                disabled={isLoading}
                onChange={(e) => {
                  setItemsPerPage(Number(e.target.value));
                  setCurrentPage(0);
                }}
              >
                {USERS_PAGE_SIZE_OPTIONS.map((option) => (
                  <option key={option} value={option}>{option} por página</option>
                ))}
              </Dropdown>
            </div>
          </footer>
        )}
      </div>

      <ConfirmModal
        isOpen={roleModalState.isOpen}
        onClose={() => setRoleModalState({ isOpen: false, targetUser: null, nextRole: null })}
        onConfirm={handleConfirmRoleChange}
        title={isPromotion ? 'Ascender a administrador' : 'Eliminar permisos de administrador'}
        message={isPromotion
          ? `¿Confirmas que deseas ascender a ${roleTargetName} a administrador?`
          : `¿Confirmas que deseas retirar los permisos de administrador a ${roleTargetName}?`}
        confirmLabel={isPromotion ? 'Ascender' : 'Eliminar permisos'}
        variant={isPromotion ? 'primary' : 'danger'}
        isProcessing={isProcessingAction}
        errorMessage={actionError}
      />

      <ConfirmModal
        isOpen={deleteModalState.isOpen}
        onClose={() => setDeleteModalState({ isOpen: false, targetUser: null })}
        onConfirm={handleConfirmDeleteUser}
        title="Eliminar cuenta"
        message={`¿Confirmas que deseas eliminar la cuenta de ${deleteTargetName}? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar cuenta"
        variant="danger"
        isProcessing={isProcessingAction}
        errorMessage={actionError}
      />
    </main>
  );
}
