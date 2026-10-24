import { useEffect, useRef, useState } from 'react';
import { MdArrowForward, MdClose, MdDeleteOutline, MdKeyboardArrowDown } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';

import {
  deleteNotification, getNotificationDestination, getNotifications,
  markAllNotificationsAsRead, markNotificationAsRead,
} from '../../api/notificationsApi.js';
import { formatDate } from '../../utils/formatDate.js';
import {
  getNotificationDestinationLabel,
  getNotificationPageSize,
} from '../../utils/notificationsModalUtils.js';
import { Skeleton } from '../Skeleton/Skeleton.jsx';
import './NotificationsModal.css';

export function NotificationsModal({ onClose }) {
  const navigate = useNavigate();
  const dialogRef = useRef(null);
  const titleRef = useRef(null);
  const actionLock = useRef(false);
  const [pageSize] = useState(getNotificationPageSize);
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [readFilter, setReadFilter] = useState('UNREAD');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const dialog = dialogRef.current;
    dialog.showModal();
    titleRef.current?.focus();
    return () => { if (dialog.open) dialog.close(); };
  }, []);

  useEffect(() => {
    let active = true;
    getNotifications({ page, size: pageSize, unreadOnly: readFilter === 'UNREAD' })
      .then((result) => {
        if (!active) return;
        setNotifications((items) => page === 0 ? result.content : [...items, ...result.content]);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);
      })
      .catch((failure) => { if (active) setError(failure.message || 'No se pudieron cargar las notificaciones.'); })
      .finally(() => { if (active) { setLoading(false); setLoadingMore(false); } });
    return () => { active = false; };
  }, [page, pageSize, readFilter, reloadKey]);

  async function runAction(action) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusy(true); setError(''); setStatus('');
    try { await action(); }
    catch (failure) { setError(failure.message || 'No se pudo completar la operación.'); }
    finally { actionLock.current = false; setBusy(false); }
  }

  function resetList(nextReadFilter = readFilter) {
    setLoading(true); setError(''); setNotifications([]); setPage(0);
    setReadFilter(nextReadFilter);
    if (nextReadFilter === readFilter) setReloadKey((key) => key + 1);
  }

  async function markRead(item) {
    const updated = await markNotificationAsRead(item.id);
    if (readFilter === 'UNREAD') {
      setNotifications((items) => items.filter(({ id }) => id !== item.id));
      setTotalElements((total) => Math.max(0, total - 1));
    } else setNotifications((items) => items.map((current) => current.id === item.id ? updated : current));
    setStatus('Notificación marcada como leída.');
  }

  function goToDestination(item) {
    void runAction(async () => {
      if (!item.readAt) await markRead(item);
      const result = await getNotificationDestination(item.id);
      if (result.url) { onClose(); navigate(result.url); }
      else setStatus('El destino ya no está disponible o no tienes acceso.');
    });
  }

  function remove(item) {
    void runAction(async () => {
      await deleteNotification(item.id);
      setNotifications((items) => items.filter(({ id }) => id !== item.id));
      setTotalElements((total) => Math.max(0, total - 1));
      setStatus('Notificación eliminada definitivamente.');
    });
  }

  const remaining = Math.max(0, totalElements - notifications.length);
  const nextCount = Math.min(pageSize, remaining);

  return (
    <dialog ref={dialogRef} className="notifications-modal" aria-labelledby="notifications-title"
      onCancel={(event) => { event.preventDefault(); onClose(); }}>
      <header className="notifications-modal-header">
        <div className="notifications-title-group">
          <h1 id="notifications-title" ref={titleRef} tabIndex={-1}>TUS NOTIFICACIONES</h1>
          <span className="notifications-count" aria-live="polite"
            aria-label={`${totalElements} notificaciones`}>{totalElements}</span>
        </div>
        <button type="button" className="notifications-close" aria-label="Cerrar notificaciones" onClick={onClose}>
          <MdClose aria-hidden="true" />
        </button>
      </header>

      <section className="notifications-filters" aria-label="Filtros de notificaciones">
        <div className="notification-status-tabs" role="group" aria-label="Filtrar por estado">
          <button type="button" aria-controls="notifications-results" aria-pressed={readFilter === 'UNREAD'}
            onClick={() => resetList('UNREAD')}>No leídas</button>
          <button type="button" aria-controls="notifications-results" aria-pressed={readFilter === 'ALL'}
            onClick={() => resetList('ALL')}>Todas</button>
        </div>
        <div className="notifications-filter-actions">
          <button type="button" className="notifications-read-all" disabled={busy || loading || !totalElements}
            onClick={() => runAction(async () => {
              await markAllNotificationsAsRead(); resetList(readFilter);
              setStatus('Todas las notificaciones se han marcado como leídas.');
            })}>Marcar todas como leídas</button>
        </div>
      </section>

      {error && <p className="notifications-error" role="alert">{error}</p>}
      {!loading && status && <p className="sr-only" role="status">{status}</p>}
      <div id="notifications-results"
        className={`notifications-modal-body${loading || !notifications.length ? ' notifications-modal-body-empty' : ''}`}>
        {loading ? <Skeleton variant="compact" count={4} hasActions={false} label="Cargando notificaciones…" /> : !notifications.length ? (
          <p className="notifications-empty">No hay notificaciones que coincidan con los filtros.</p>
        ) : (
          <ul className="notifications-list" aria-busy={busy || loadingMore}>
            {notifications.map((item) => {
              const date = formatDate(item.createdAt, { includeTime: true });
              const destinationLabel = getNotificationDestinationLabel(item.targetType);
              return (
                <li key={item.id} className={item.readAt ? '' : 'notification-unread'}>
                  <div className="notification-content">
                    <div className="notification-title-row">
                      <h2>{item.title}</h2>
                      {!item.readAt && <span className="notification-new"><span aria-hidden="true" />Nueva</span>}
                      {item.readAt && <button type="button" className="notification-delete" disabled={busy}
                        aria-label={`Eliminar definitivamente: ${item.title}`} onClick={() => remove(item)}>
                        <MdDeleteOutline aria-hidden="true" />
                      </button>}
                    </div>
                    <p className="notification-description">{item.message}</p>
                    <div className="notification-footer">
                      <p className="notification-meta"><time dateTime={item.createdAt}>{date.date} · {date.time}</time>
                        {item.channel === 'EMAIL' && <span>Alta prioridad</span>}
                      </p>
                      <div className="notification-actions">
                        {destinationLabel && <button type="button" className="notification-manage" disabled={busy}
                          onClick={() => goToDestination(item)}>{destinationLabel}<MdArrowForward aria-hidden="true" /></button>}
                        {!item.readAt && <button type="button" className="notification-read" disabled={busy}
                          onClick={() => runAction(() => markRead(item))}>Marcar como leída</button>}
                      </div>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
        {remaining > 0 && page + 1 < totalPages && <button type="button" className="notifications-more"
          disabled={loadingMore || busy} onClick={() => { setLoadingMore(true); setPage((current) => current + 1); }}>
          <MdKeyboardArrowDown aria-hidden="true" />{loadingMore ? 'Cargando…' : `Ver ${nextCount} más`}
        </button>}
      </div>
    </dialog>
  );
}
