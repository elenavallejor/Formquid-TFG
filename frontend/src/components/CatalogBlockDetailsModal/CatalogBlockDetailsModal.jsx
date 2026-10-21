import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { MdClose } from 'react-icons/md';

import { getCatalogBlockById } from '../../api/catalogApi.js';
import { useModalFocus } from '../../hooks/useModalFocus.js';
import { formatDate } from '../../utils/formatDate.js';
import { Block } from '../Block/Block.jsx';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './CatalogBlockDetailsModal.css';

function formatDateTime(value) {
  const { date, time } = formatDate(value, { includeTime: true });
  return time ? `${date}, ${time}` : date;
}

export function CatalogBlockDetailsModal({ block, onClose }) {
  const dialogRef = useRef(null);
  const closeRef = useRef(null);
  const [details, setDetails] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [retryCount, setRetryCount] = useState(0);

  useModalFocus({ isOpen: Boolean(block), onClose, dialogRef, initialFocusRef: closeRef });

  useEffect(() => {
    if (!block) return undefined;
    let active = true;
    getCatalogBlockById(block.id)
      .then((response) => {
        if (active) setDetails(response);
      })
      .catch((loadError) => {
        if (active) setError(loadError.message || 'No se han podido cargar los detalles del bloque.');
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => { active = false; };
  }, [block, retryCount]);

  if (!block) return null;

  function retry() {
    setDetails(null);
    setError('');
    setIsLoading(true);
    setRetryCount((count) => count + 1);
  }

  return createPortal(
    <div className="catalog-block-details-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section ref={dialogRef} tabIndex={-1} className="catalog-block-details-modal" role="dialog" aria-modal="true" aria-labelledby="catalog-block-details-title">
        <header className="catalog-block-details-header">
          <div>
            <span>Catálogo de elementos</span>
            <h2 id="catalog-block-details-title">DETALLES DEL BLOQUE</h2>
          </div>
          <button ref={closeRef} type="button" aria-label="Cerrar detalles del bloque" onClick={onClose}><MdClose aria-hidden="true" /></button>
        </header>

        <div className="catalog-block-details-body">
          {isLoading && <Skeleton variant="list" count={3} hasActions={false} />}
          {!isLoading && error && (
            <div className="catalog-block-details-error" role="alert">
              <p>{error}</p>
              <button type="button" onClick={retry}>Reintentar</button>
            </div>
          )}
          {!isLoading && details && (
            <>
              <dl className="catalog-block-details-metadata">
                <div><dt>CREADOR</dt><dd>{details.creatorName || '—'}</dd></div>
                <div><dt>CREADO</dt><dd>{formatDateTime(details.createdAt)}</dd></div>
                <div><dt>ÚLTIMA ACTUALIZACIÓN</dt><dd>{formatDateTime(details.updatedAt)}</dd></div>
              </dl>
              <Block data={details} headingLevel={3} />
            </>
          )}
        </div>
      </section>
    </div>,
    document.body,
  );
}
