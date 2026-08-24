import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { MdSearch, MdKeyboardArrowRight } from 'react-icons/md';

import { TarjetaSolicitud } from '../../../components/TarjetaSolicitud/TarjetaSolicitud.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import {
  getPendingRegistrationRequests,
  approveRegistrationRequest,
  rejectRegistrationRequest,
} from '../../../api/registrationRequestsApi.js';

import './RegistrationRequestsPage.css';

export function RegistrationRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('NEWEST');

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
        const data = await getPendingRegistrationRequests();
        if (isMounted) {
          const items = Array.isArray(data) ? data : data?.content || [];
          setRequests(items);
        }
      } catch (err) {
        if (isMounted) {
          setErrorMessage('No se pudieron obtener las solicitudes de registro.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void fetchRequests();

    return () => {
      isMounted = false;
    };
  }, []);

  function handleOpenModal(actionType, solicitud) {
    setModalState({
      isOpen: true,
      actionType,
      solicitud,
    });
  }

  function handleCloseModal() {
    setModalState({ isOpen: false, actionType: null, solicitud: null });
  }

  async function handleConfirmAction() {
    const { actionType, solicitud } = modalState;
    if (!solicitud) return;

    try {
      if (actionType === 'ACCEPT') {
        await approveRegistrationRequest(solicitud.id);
      } else if (actionType === 'DENY') {
        await rejectRegistrationRequest(solicitud.id);
      }

      setRequests((prev) => prev.filter((item) => item.id !== solicitud.id));
    } catch (err) {
      console.error('Error al procesar la solicitud:', err);
    } finally {
      handleCloseModal();
    }
  }

  const filteredRequests = useMemo(() => {
    return requests
      .filter((req) => {
        const query = searchQuery.toLowerCase();
        return (
          req.name?.toLowerCase().includes(query) ||
          req.email?.toLowerCase().includes(query) ||
          req.institution?.toLowerCase().includes(query)
        );
      })
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
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="sort-selector-wrapper">
            <label htmlFor="sort-select">Ordenar por:</label>
            <select
              id="sort-select"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
            >
              <option value="NEWEST">Más reciente</option>
              <option value="OLDEST">Más antigua</option>
            </select>
          </div>
        </section>

        <section className="requests-list" aria-label="Listado de solicitudes">
          {isLoading ? (
            <p className="requests-status-text">Cargando solicitudes...</p>
          ) : errorMessage ? (
            <p className="requests-error-text">{errorMessage}</p>
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
      />
    </main>
  );
}