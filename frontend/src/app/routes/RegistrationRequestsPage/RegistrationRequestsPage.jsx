import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { MdSearch, MdKeyboardArrowRight } from 'react-icons/md';

import { TarjetaSolicitud } from '../../../components/TarjetaSolicitud/TarjetaSolicitud';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal';
import { getPendingRegistrationRequests, approveRegistrationRequest, rejectRegistrationRequest } from '../../../api/registrationRequestsApi';

import './RegistrationRequestsPage.css';

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
      .filter((request) => (
        request.name?.toLowerCase().includes(query) ||
        request.email?.toLowerCase().includes(query) ||
        request.institution?.toLowerCase().includes(query)
      ))
      .sort((a, b) => {
        const dateA = new Date(a.createdAt).getTime();
        const dateB = new Date(b.createdAt).getTime();
        return sortOrder === 'NEWEST' ? dateB - dateA : dateA - dateB;
      });
  }, [requests, searchQuery, sortOrder]);

  return (
    <main className="admin-requests-page">
      <div className="admin-requests-container">
        <nav className="breadcrumb-nav" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdKeyboardArrowRight aria-hidden="true" />
          <span aria-current="page">SOLICITUDES DE REGISTRO</span>
        </nav>

        <header className="page-header">
          <h1>SOLICITUDES DE REGISTRO</h1>
          <p>Revisa y gestiona las solicitudes de alta de nuevos editores en la plataforma.</p>
        </header>

        <section className="filters-bar" aria-label="Filtros de búsqueda">
          <div className="search-input-wrapper">
            <MdSearch className="search-icon" aria-hidden="true" />
            <input
              type="text"
              placeholder="Buscar por nombre, correo o institución..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>

          <div className="sort-selector-wrapper">
            <label htmlFor="sort-select">Ordenar por:</label>
            <select id="sort-select" value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
              <option value="NEWEST">Más reciente</option>
              <option value="OLDEST">Más antigua</option>
            </select>
          </div>
        </section>

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
        actionType={modalState.actionType}
        applicantName={modalState.solicitud?.name}
        isProcessing={isProcessing}
        errorMessage={actionError}
      />
    </main>
  );
}