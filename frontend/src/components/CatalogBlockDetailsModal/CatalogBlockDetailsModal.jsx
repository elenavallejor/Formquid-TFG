import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { MdClose, MdOutlineWidgets } from 'react-icons/md';

import { getCatalogBlockById } from '../../api/catalogApi.js';
import { useModalFocus } from '../../hooks/useModalFocus.js';
import { formatDateTime } from '../../utils/formatDate.js';
import { Skeleton } from '../Skeleton/Skeleton.jsx';
import { CatalogBlockContent } from './CatalogBlockContent.jsx';

import '../BlockBuilder/BlockBuilder.css';
import '../../app/routes/ElementDetailPage/ElementDetailPage.css';
import './CatalogBlockDetailsModal.css';

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
      <section ref={dialogRef} tabIndex={-1} className="catalog-block-details-modal" role="dialog" aria-modal="true" aria-label="DETALLES DEL BLOQUE">
        <div className="catalog-block-details-body">
          <header className="catalog-detail-page-header catalog-block-details-header">
            <div>
              <h1>VER DETALLES:</h1>
              <p>Aquí puedes ver los detalles del elemento</p>
            </div>
            <button ref={closeRef} type="button" aria-label="Cerrar detalles del bloque" onClick={onClose}><MdClose aria-hidden="true" /></button>
          </header>

          {isLoading && <div className="catalog-detail-loading"><Skeleton variant="list" count={3} hasActions={false} /></div>}
          {!isLoading && error && (
            <div className="catalog-detail-error catalog-block-details-error" role="alert">
              <p>{error}</p>
              <button type="button" onClick={retry}>Reintentar</button>
            </div>
          )}
          {!isLoading && details && (
            <div className="catalog-detail-card">
              <header className="catalog-detail-title-bar">
                <span className="catalog-detail-title-icon" aria-hidden="true"><MdOutlineWidgets /></span>
                <h2>{(details.name || 'Sin bloque').toUpperCase()}</h2>
              </header>

              <div className="catalog-detail-body">
                <p className="catalog-detail-description">{details.description || 'Sin descripción.'}</p>

                <dl className="catalog-detail-metadata">
                  <div><dt>CREADOR</dt><dd>{details.creatorName || '—'}</dd></div>
                  <div><dt>CREADO</dt><dd>{formatDateTime(details.createdAt)}</dd></div>
                  <div><dt>ÚLTIMA ACTUALIZACIÓN</dt><dd>{formatDateTime(details.updatedAt)}</dd></div>
                </dl>

                <section className="catalog-detail-structure catalog-detail-structure--block" aria-labelledby="catalog-block-details-structure-title">
                  <header className="catalog-detail-structure-header">
                    <div><h3 id="catalog-block-details-structure-title">ESTRUCTURA DEL BLOQUE</h3></div>
                  </header>
                  <div className="catalog-detail-structure-content"><CatalogBlockContent element={details} /></div>
                </section>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>,
    document.body,
  );
}
