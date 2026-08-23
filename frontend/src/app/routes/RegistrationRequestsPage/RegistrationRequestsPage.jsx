// src/pages/admin/RegistrationRequestsPage/RegistrationRequestsPage.jsx
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

  // Estado para el modal de confirmación
  const [modalState, setModalState] = useState({
    isOpen: false,
    actionType: null, // 'ACCEPT' | 'DENY'
    solicitud: null,
  });

  useEffect(() => {
    fetchRequests();
  }, []);

  async function fetchRequests() {
    try {
      setIsLoading(true);
      const data = await getPendingRegistrationRequests();
      const items = Array.isArray(data) ? data : data?.content || [];
      setRequests(items);
    } catch (err) {
      setErrorMessage('No se pudieron obtener las solicitudes de registro.');
    } finally {
      setIsLoading(false);
    }
  }

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

      // Elimina la solicitud de la lista inmediatamente tras confirmar
      setRequests((prev) => prev.filter((item) => item.id !== solicitud.id));
    } catch (err) {
      console.error('Error al procesar la solicitud:', err);
    } finally {
      handleCloseModal();
    }
  }

  // Filtrado y ordenación
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
        {/* Migas de pan */}
        <nav className="breadcrumb-nav" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdKeyboardArrowRight aria-hidden="true" />
          <span aria-current="page">SOLICITUDES DE REGISTRO</span>
        </nav>

        {/* Encabezado */}
        <header className="page-header">
          <h1>SOLICITUDES DE REGISTRO</h1>
          <p>Revisa y gestiona las solicitudes de alta de nuevos editores en la plataforma.</p>
        </header>

        {/* Barra de Filtros */}
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

        {/* Listado de Solicitudes */}
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

      {/* Modal de Confirmación */}
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