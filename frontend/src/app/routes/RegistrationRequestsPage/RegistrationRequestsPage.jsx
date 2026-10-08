import { useEffect, useMemo, useRef, useState } from 'react';
import { MdKeyboardArrowRight } from 'react-icons/md';

import { TarjetaSolicitud } from '../../../components/TarjetaSolicitud/TarjetaSolicitud';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { getPendingRegistrationRequests, approveRegistrationRequest, rejectRegistrationRequest } from '../../../api/registrationRequestsApi';

import './RegistrationRequestsPage.css';

const SEARCH_OPTIONS = [
  { value: 'NAME', label: 'Nombre' },
  { value: 'EMAIL', label: 'Correo' },
  { value: 'INSTITUTION', label: 'Institución' },
];
const SORT_OPTIONS = [
  { value: 'NEWEST', label: 'Más reciente' },
  { value: 'OLDEST', label: 'Más antigua' },
];

/**
 * Página de gestión de solicitudes de registro. ADMIN y SUPERADMIN
 * 
 * Muestra y gestiona las solicitudes de registro pendientes, 
 * permitiendo buscarlas, ordenarlas, aprobarlas o rechazarlas.
 */
export function RegistrationRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSearchFields, setSelectedSearchFields] = useState(
    () => SEARCH_OPTIONS.map((option) => option.value),
  );
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [isProcessing, setIsProcessing] = useState(false);
  const [actionError, setActionError] = useState('');

  const actionLockRef = useRef(false);

  const [modalState, setModalState] = useState({
    isOpen: false,
    actionType: null,
    solicitud: null,
  });

  useEffect(() => {
    let isMounted = true;

    async function fetchRequests() {
      try {
        setIsLoading(true);
        setErrorMessage('');

        const data = await getPendingRegistrationRequests();

        if (isMounted) {
          const items = Array.isArray(data) ? data : data?.content || [];
          setRequests(items);
        }
      } catch {
        if (isMounted) setErrorMessage('No se pudieron obtener las solicitudes de registro.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void fetchRequests();

    return () => {
      isMounted = false;
    };
  }, []);

  function handleOpenModal(actionType, solicitud) {
    if (actionLockRef.current) return;

    setActionError('');
    setModalState({ isOpen: true, actionType, solicitud });
  }

  function handleCloseModal() {
    if (actionLockRef.current) return;

    setActionError('');
    setModalState({ isOpen: false, actionType: null, solicitud: null });
  }

  async function handleConfirmAction() {
    const { actionType, solicitud } = modalState;
    if (!solicitud || actionLockRef.current) return;

    actionLockRef.current = true;
    setIsProcessing(true);
    setActionError('');

    try {
      if (actionType === 'ACCEPT') {
        await approveRegistrationRequest(solicitud.id);
      } else if (actionType === 'DENY') {
        await rejectRegistrationRequest(solicitud.id);
      } else {
        return;
      }

      setRequests((previous) => previous.filter((item) => item.id !== solicitud.id));
      setModalState({ isOpen: false, actionType: null, solicitud: null });
    } catch (error) {
      console.error('Error al procesar la solicitud:', error);
      setActionError('No se ha podido procesar la solicitud. Inténtalo de nuevo.');
    } finally {
      actionLockRef.current = false;
      setIsProcessing(false);
    }
  }

  const filteredRequests = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();

    return requests
      .filter((request) => !query || (
        (selectedSearchFields.includes('NAME') && request.name?.toLowerCase().includes(query)) ||
        (selectedSearchFields.includes('EMAIL') && request.email?.toLowerCase().includes(query)) ||
        (selectedSearchFields.includes('INSTITUTION') && request.institution?.toLowerCase().includes(query))
      ))
      .sort((a, b) => {
        const dateA = new Date(a.createdAt).getTime();
        const dateB = new Date(b.createdAt).getTime();
        return sortOrder === 'NEWEST' ? dateB - dateA : dateA - dateB;
      });
  }, [requests, searchQuery, selectedSearchFields, sortOrder]);

  const isAcceptAction = modalState.actionType === 'ACCEPT';
  const applicantName = modalState.solicitud?.name || 'este usuario';

  return (
    <main className="admin-requests-page">
      <div className="admin-requests-container">
        <Breadcrumbs
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'SOLICITUDES DE REGISTRO' },
          ]}
          separator={MdKeyboardArrowRight}
        />

        <header className="page-header">
          <h1>SOLICITUDES DE REGISTRO</h1>
          <p>Revisa y gestiona las solicitudes de alta de nuevos editores en la plataforma.</p>
        </header>

        <SearchToolbar
          ariaLabel="Filtros de solicitudes de registro"
          searchLabel="Introduce una palabra para buscar solicitudes de registro"
          searchValue={searchQuery}
          onSearchChange={(event) => setSearchQuery(event.target.value)}
          filterOptions={SEARCH_OPTIONS}
          selectedFilterValues={selectedSearchFields}
          onFilterChange={setSelectedSearchFields}
          onFilterReset={() => setSearchQuery('')}
          sortAriaLabel="Ordenar solicitudes de registro"
          sortOptions={SORT_OPTIONS}
          sortValue={sortOrder}
          onSortChange={(event) => setSortOrder(event.target.value)}
        />

        <section className="requests-list" aria-label="Listado de solicitudes">
          {isLoading ? (
            <p className="requests-status-text">Cargando solicitudes...</p>
          ) : errorMessage ? (
            <p className="requests-error-text" role="alert">{errorMessage}</p>
          ) : filteredRequests.length === 0 ? (
            <div className="empty-requests-card">
              <p>No se han encontrado solicitudes de registro pendientes.</p>
            </div>
          ) : (
            filteredRequests.map((solicitud) => (
              <TarjetaSolicitud
                key={solicitud.id}
                solicitud={solicitud}
                onAccept={(item) => handleOpenModal('ACCEPT', item)}
                onDeny={(item) => handleOpenModal('DENY', item)}
              />
            ))
          )}
        </section>
      </div>

      <ConfirmModal
        isOpen={modalState.isOpen}
        onClose={handleCloseModal}
        onConfirm={handleConfirmAction}
        title={isAcceptAction ? 'Aceptar solicitud' : 'Rechazar solicitud'}
        message={`¿Confirmas que deseas ${isAcceptAction ? 'aceptar' : 'rechazar'} la solicitud de ${applicantName}?`}
        confirmLabel={isAcceptAction ? 'Aceptar' : 'Rechazar'}
        variant={isAcceptAction ? 'primary' : 'danger'}
        isProcessing={isProcessing}
        errorMessage={actionError}
      />
    </main>
  );
}
