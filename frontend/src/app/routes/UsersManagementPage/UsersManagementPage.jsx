import { useEffect, useRef, useState } from 'react';
import { MdKeyboardArrowRight, MdMailOutline, MdOutlineBusiness, MdPublic } from 'react-icons/md';

import { deleteUser, getUsers, updateUserRole } from '../../../api/usersApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../../context/useAuth.js';
import { getCountryName } from '../../../utils/countries.js';
import { DEFAULT_USERS_PAGE_SIZE, USER_SEARCH_OPTIONS, USER_SORT_OPTIONS, USERS_PAGE_SIZE_OPTIONS, canChangeUserRole, canDeleteUser, getNextRole, getUserRoleLabel } from '../../../utils/usersManagementUtils.js';

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
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedSearchFields, setSelectedSearchFields] = useState(
    () => USER_SEARCH_OPTIONS.map((option) => option.value),
  );
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
    const timeoutId = window.setTimeout(() => setDebouncedSearch(searchQuery.trim()), 300);
    return () => window.clearTimeout(timeoutId);
  }, [searchQuery]);

  useEffect(() => {
    const requestVersion = ++requestVersionRef.current;

    async function loadUsers() {
      try {
        setIsLoading(true);
        setErrorMessage('');
        setUsersList([]);

        const response = await getUsers({
          page: currentPage,
          size: itemsPerPage,
          search: debouncedSearch,
          searchFields: selectedSearchFields,
          sortOrder,
        });

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
  }, [currentPage, debouncedSearch, itemsPerPage, selectedSearchFields, sortOrder]);

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

        <SearchToolbar
          ariaLabel="Filtros de usuarios"
          searchLabel="Introduce una palabra para buscar usuarios"
          searchValue={searchQuery}
          onSearchChange={(event) => {
            setSearchQuery(event.target.value);
            setCurrentPage(0);
          }}
          filterOptions={USER_SEARCH_OPTIONS}
          filterOptionsControl="multiselect"
          filterOptionsAllLabel="Todos los campos"
          filterOptionsNoneLabel="Ningún campo"
          filterOptionsSelectedCountLabel="campos seleccionados"
          filterLabel="Filtrar"
          filterOptionsLabel="Campos de búsqueda de usuarios"
          selectedFilterValues={selectedSearchFields}
          showFilter
          onFilterChange={(fields) => {
            setSelectedSearchFields(fields);
            setCurrentPage(0);
          }}
          onFilterReset={() => {
            setSearchQuery('');
            setDebouncedSearch('');
          }}
          sortAriaLabel="Ordenar usuarios"
          sortOptions={USER_SORT_OPTIONS}
          sortValue={sortOrder}
          onSortChange={(event) => {
            setSortOrder(event.target.value);
            setCurrentPage(0);
          }}
        />

        {isLoading ? (
          <div className="users-list-wrapper">
            <Skeleton variant="list" count={itemsPerPage} hasActions={true} />
          </div>
        ) : errorMessage ? (
          <div className="users-empty-state" role="alert">
            <p>{errorMessage}</p>
          </div>
        ) : usersList.length === 0 ? (
          <div className="users-empty-state">
            <p>No se han encontrado usuarios que coincidan con la búsqueda.</p>
          </div>
        ) : (
          <div className="users-list-wrapper">
            {usersList.map((item) => {
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
                        <span className="user-detail-item"><MdPublic aria-hidden="true" />{getCountryName(item.country)}</span>
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
          <Pagination
            ariaLabel="Paginación de usuarios"
            currentPage={currentPage}
            itemLabel="usuarios"
            onPageChange={setCurrentPage}
            onPageSizeChange={(size) => {
              setItemsPerPage(size);
              setCurrentPage(0);
            }}
            pageSize={itemsPerPage}
            pageSizeLabel="Usuarios por página"
            pageSizeOptions={USERS_PAGE_SIZE_OPTIONS}
            totalItems={totalItems}
            totalPages={totalPages}
          />
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
