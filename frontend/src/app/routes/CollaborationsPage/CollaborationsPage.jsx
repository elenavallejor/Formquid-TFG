import { useEffect, useMemo, useState } from 'react';

import {
  acceptCollaborationRequest,
  getCollaborationOverview,
  rejectCollaborationRequest,
} from '../../../api/collaborationsApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { Table } from '../../../components/Table/Table.jsx';
import { useAuth } from '../../../context/useAuth.js';

import './CollaborationsPage.css';

const PERMISSION_LABELS = {
  EDIT: 'Puede editar',
  VIEW: 'Solo lectura',
};

const DATE_FORMATTER = new Intl.DateTimeFormat('es-ES', {
  dateStyle: 'medium',
});

function formatDate(value) {
  return value ? DATE_FORMATTER.format(new Date(value)) : '—';
}

function Person({ name, email }) {
  return (
    <span className="collaborations-person">
      <strong>{name || 'Usuario'}</strong>
      <span>{email || 'Sin correo disponible'}</span>
    </span>
  );
}

function PendingBadge() {
  return <span className="collaborations-badge">Pendiente</span>;
}

function ResolutionActions({ request, pendingId, onAccept, onReject }) {
  const type = request.requestType === 'INVITATION' ? 'invitación' : 'solicitud';
  const person = request.requestType === 'INVITATION' ? request.ownerName : request.userName;
  const label = `${type} de ${person || 'usuario'} para ${request.surveyTitle}`;

  return (
    <span className="collaborations-actions">
      <button
        type="button"
        disabled={pendingId !== null}
        aria-label={`Aceptar ${label}`}
        onClick={() => onAccept(request)}
      >
        {pendingId === request.id ? 'Procesando...' : 'Aceptar'}
      </button>
      <button
        type="button"
        className="collaborations-reject"
        disabled={pendingId !== null}
        aria-label={`Rechazar ${label}`}
        onClick={() => onReject(request)}
      >
        Rechazar
      </button>
    </span>
  );
}

function RequestSection({ id, title, requests, headers, rows, emptyMessage }) {
  return (
    <section className="collaborations-section" aria-labelledby={id}>
      <div className="collaborations-section-heading">
        <h2 id={id}>{title}</h2>
        <span aria-label={`${requests.length} elementos`}>{requests.length}</span>
      </div>
      <Table
        caption={`${title} del usuario`}
        headers={headers}
        data={rows}
        emptyMessage={emptyMessage}
      />
    </section>
  );
}

export function CollaborationsPage() {
  const { user } = useAuth();
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingId, setPendingId] = useState(null);
  const [requestToReject, setRequestToReject] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (!user?.id) return undefined;
    let isActive = true;

    getCollaborationOverview()
      .then((response) => {
        if (isActive) setRequests(Array.isArray(response) ? response : []);
      })
      .catch((error) => {
        if (isActive) setErrorMessage(error.message || 'No se han podido cargar las colaboraciones.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => { isActive = false; };
  }, [user?.id]);

  const groupedRequests = useMemo(() => ({
    receivedRequests: requests.filter((request) => request.requestType === 'ACCESS_REQUEST' && request.ownerId === user?.id),
    sentRequests: requests.filter((request) => request.requestType === 'ACCESS_REQUEST' && request.userId === user?.id),
    receivedInvitations: requests.filter((request) => request.requestType === 'INVITATION' && request.userId === user?.id),
    sentInvitations: requests.filter((request) => request.requestType === 'INVITATION' && request.ownerId === user?.id),
  }), [requests, user?.id]);

  async function resolveRequest(request, accept) {
    const type = request.requestType === 'INVITATION' ? 'invitación' : 'solicitud';
    setPendingId(request.id);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const action = accept ? acceptCollaborationRequest : rejectCollaborationRequest;
      await action(request.surveyId, request.id);
      setRequests((current) => current.filter(({ id }) => id !== request.id));
      setSuccessMessage(`${type === 'invitación' ? 'Invitación' : 'Solicitud'} ${accept ? 'aceptada' : 'rechazada'}.`);
      if (!accept) setRequestToReject(null);
    } catch (error) {
      setErrorMessage(error.message || `No se ha podido resolver la ${type}.`);
    } finally {
      setPendingId(null);
    }
  }

  const receivedRequestRows = groupedRequests.receivedRequests.map((request) => [
    request.surveyTitle,
    <Person key="requester" name={request.userName} email={request.userEmail} />,
    PERMISSION_LABELS[request.permission] || request.permission,
    request.message || '—',
    formatDate(request.createdAt),
    <ResolutionActions key="actions" request={request} pendingId={pendingId} onAccept={(item) => resolveRequest(item, true)} onReject={setRequestToReject} />,
  ]);

  const sentRequestRows = groupedRequests.sentRequests.map((request) => [
    request.surveyTitle,
    <Person key="owner" name={request.ownerName} email={request.ownerEmail} />,
    PERMISSION_LABELS[request.permission] || request.permission,
    request.message || '—',
    formatDate(request.createdAt),
    <PendingBadge key="status" />,
  ]);

  const receivedInvitationRows = groupedRequests.receivedInvitations.map((request) => [
    request.surveyTitle,
    <Person key="owner" name={request.ownerName} email={request.ownerEmail} />,
    PERMISSION_LABELS[request.permission] || request.permission,
    request.message || '—',
    formatDate(request.createdAt),
    <ResolutionActions key="actions" request={request} pendingId={pendingId} onAccept={(item) => resolveRequest(item, true)} onReject={setRequestToReject} />,
  ]);

  const sentInvitationRows = groupedRequests.sentInvitations.map((request) => [
    request.surveyTitle,
    <Person key="guest" name={request.userName} email={request.userEmail} />,
    PERMISSION_LABELS[request.permission] || request.permission,
    request.message || '—',
    formatDate(request.createdAt),
    <PendingBadge key="status" />,
  ]);

  return (
    <main className="collaborations-page">
      <div className="collaborations-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'COLABORACIONES' }]} />

        <header className="collaborations-header">
          <h1>GESTIÓN DE COLABORACIONES</h1>
          <p>Consulta y resuelve las solicitudes e invitaciones pendientes de tus encuestas.</p>
        </header>

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}
        {successMessage && <p className="collaborations-success" role="status">{successMessage}</p>}

        <div className="collaborations-content" aria-busy={isLoading}>
          {isLoading ? (
            <Skeleton variant="table" count={4} label="Cargando colaboraciones…" />
          ) : (
            <>
              <RequestSection
                id="received-requests-title"
                title="SOLICITUDES RECIBIDAS"
                requests={groupedRequests.receivedRequests}
                headers={['Encuesta', 'Solicitante', 'Permiso', 'Mensaje', 'Fecha', 'Acciones']}
                rows={receivedRequestRows}
                emptyMessage="No has recibido solicitudes de colaboración pendientes."
              />
              <RequestSection
                id="sent-requests-title"
                title="SOLICITUDES ENVIADAS"
                requests={groupedRequests.sentRequests}
                headers={['Encuesta', 'Propietario', 'Permiso', 'Mensaje', 'Fecha', 'Estado']}
                rows={sentRequestRows}
                emptyMessage="No has enviado solicitudes de colaboración pendientes."
              />
              <RequestSection
                id="received-invitations-title"
                title="INVITACIONES RECIBIDAS"
                requests={groupedRequests.receivedInvitations}
                headers={['Encuesta', 'Propietario', 'Permiso', 'Mensaje', 'Fecha', 'Acciones']}
                rows={receivedInvitationRows}
                emptyMessage="No has recibido invitaciones pendientes."
              />
              <RequestSection
                id="sent-invitations-title"
                title="INVITACIONES ENVIADAS"
                requests={groupedRequests.sentInvitations}
                headers={['Encuesta', 'Editor invitado', 'Permiso', 'Mensaje', 'Fecha', 'Estado']}
                rows={sentInvitationRows}
                emptyMessage="No has enviado invitaciones pendientes."
              />
            </>
          )}
        </div>
      </div>

      <ConfirmModal
        isOpen={Boolean(requestToReject)}
        title={`RECHAZAR ${requestToReject?.requestType === 'INVITATION' ? 'INVITACIÓN' : 'SOLICITUD'}`}
        message={`¿Quieres rechazar esta ${requestToReject?.requestType === 'INVITATION' ? 'invitación' : 'solicitud'} para ${requestToReject?.surveyTitle || 'esta encuesta'}?`}
        confirmLabel={`Rechazar ${requestToReject?.requestType === 'INVITATION' ? 'invitación' : 'solicitud'}`}
        variant="danger"
        isProcessing={pendingId === requestToReject?.id}
        onClose={() => setRequestToReject(null)}
        onConfirm={() => resolveRequest(requestToReject, false)}
      />
    </main>
  );
}
