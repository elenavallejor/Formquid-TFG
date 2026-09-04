import { useEffect, useRef, useState } from 'react';
import { MdMoreVert, MdOutlineDelete, MdOutlineEdit } from 'react-icons/md';

import { formatDate } from '../../utils/formatDate.js';
import { getUserInitials } from '../../utils/getUserInitials.js';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './CatalogList.css';

const QUESTION_TYPE_LABELS = {
  SHORT_TEXT: 'Texto corto',
  LONG_TEXT: 'Texto largo',
  NUMBER: 'Número',
  SINGLE_CHOICE: 'Opción única',
  MULTIPLE_CHOICE: 'Opción múltiple',
  SCALE: 'Escala',
  MATRIX_SCALE: 'Matriz',
  BIPOLAR_SCALE: 'Escala bipolar',
  BOOLEAN: 'Sí / No',
};

export function CatalogList({
  items,
  config,
  isLoading,
  errorMessage,
  viewMode,
  canManageCatalog,
  itemsPerPage,
  onEdit,
  onDelete,
}) {
  const [openRowMenuId, setOpenRowMenuId] = useState(null);
  const actionMenuRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (actionMenuRef.current && !actionMenuRef.current.contains(event.target)) {
        setOpenRowMenuId(null);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (isLoading && items.length === 0) {
    return (
      <Skeleton
        variant={viewMode === 'GRID' ? 'grid' : 'list'}
        count={itemsPerPage}
        hasActions={canManageCatalog}
      />
    );
  }

  if (errorMessage) {
    return (
      <section className="catalog-list-section">
        <p className="catalog-error-text" role="alert">
          {errorMessage}
        </p>
      </section>
    );
  }

  if (items.length === 0) {
    return (
      <section className="catalog-list-section">
        <div className="empty-catalog-card">
          <p>No se encontraron {config.label.toLowerCase()} disponibles.</p>
        </div>
      </section>
    );
  }

  const ItemIcon = config.icon;

  function renderActions(item) {
    if (!canManageCatalog) return null;

    return (
      <div className="catalog-actions-cell">
        <button
          type="button"
          className="btn-item-more"
          aria-label="Opciones"
          onClick={(e) => {
            e.stopPropagation();
            setOpenRowMenuId(openRowMenuId === item.id ? null : item.id);
          }}
        >
          <MdMoreVert aria-hidden="true" />
        </button>

        {openRowMenuId === item.id && (
          <div className="catalog-action-menu" ref={actionMenuRef}>
            <button
              type="button"
              className="menu-option"
              onClick={() => {
                setOpenRowMenuId(null);
                onEdit(item.id);
              }}
            >
              <MdOutlineEdit aria-hidden="true" />
              <span>Editar</span>
            </button>
            <button
              type="button"
              className="menu-option option-delete"
              onClick={() => {
                setOpenRowMenuId(null);
                onDelete(item);
              }}
            >
              <MdOutlineDelete aria-hidden="true" />
              <span>Borrar</span>
            </button>
          </div>
        )}
      </div>
    );
  }

  if (viewMode === 'GRID') {
    return (
      <section className="catalog-list-section">
        <div className="catalog-grid-body">
          {items.map((item) => {
            const title = item[config.titleKey] || 'Sin título';
            const description = item[config.descriptionKey] || 'Sin descripción.';
            const creator = item.creatorName || '';
            const updatedAt = formatDate(item.updatedAt, { includeTime: true });

            const isQuestion = config.detailKey === 'questionType';
            const detailValue = isQuestion
              ? QUESTION_TYPE_LABELS[item.questionType] ?? item.questionType ?? '—'
              : item[config.detailKey] ?? 0;
            const detailPrefix = isQuestion ? 'Tipo: ' : 'Preguntas: ';

            return (
              <article key={item.id} className="catalog-grid-card">
                <div className="card-top">
                  <div className="catalog-item-icon">
                    <ItemIcon aria-hidden="true" />
                  </div>
                  {renderActions(item)}
                </div>
                <div className="card-body">
                  <h3>{title}</h3>
                  <p>{description}</p>
                </div>
                <div className="card-footer">
                  <div className="user-creator-cell grid-creator-cell">
                    <span className="user-creator-name">{creator || '—'}</span>
                  </div>
                  <div className="card-metrics font-numeric">
                    <span>
                      <strong>{detailPrefix}</strong>
                      {detailValue}
                    </span>
                    <span>
                      <strong>Actualizado: </strong>
                      {updatedAt.date}
                    </span>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    );
  }

  return (
    <section
      className="catalog-list-section catalog-list-section--fixed"
      style={{ '--catalog-page-size': itemsPerPage }}
    >
      <div className="catalog-blocks-wrapper">
        <div className={`catalog-list-columns-header ${!canManageCatalog ? 'no-actions' : ''}`}>
          <span className="col-main-header">{config.singularLabel.toUpperCase()}</span>
          <span className="col-creator-header">CREADOR</span>
          <span className="col-count-header">{config.detailLabel.toUpperCase()}</span>
          <span className="col-updated-header">ACTUALIZADO</span>
          {canManageCatalog && <span className="col-actions-header">ACCIONES</span>}
        </div>

        <div className="catalog-blocks-list">
          {items.map((item) => {
            const title = item[config.titleKey] || 'Sin título';
            const description = item[config.descriptionKey] || 'Sin descripción.';
            const creator = item.creatorName || '';
            const updatedAt = formatDate(item.updatedAt, { includeTime: true });
            const detailValue =
              config.detailKey === 'questionType'
                ? QUESTION_TYPE_LABELS[item.questionType] ?? item.questionType ?? '—'
                : item[config.detailKey] ?? 0;

            return (
              <article
                key={item.id}
                className={`catalog-block-card ${!canManageCatalog ? 'no-actions' : ''}`}
              >
                <div className="col-main catalog-item-main">
                  <div className="catalog-item-icon">
                    <ItemIcon aria-hidden="true" />
                  </div>
                  <div className="catalog-item-info">
                    <h3>{title}</h3>
                    <p>{description}</p>
                  </div>
                </div>
                <div className="col-creator user-creator-cell">
                  <span className="user-avatar-circle" aria-hidden="true">
                    {getUserInitials(creator)}
                  </span>
                  <span className="user-creator-name">{creator || '—'}</span>
                </div>
                <div className="col-count font-numeric">{detailValue}</div>
                <div className="col-updated font-numeric font-date">
                  <div>{updatedAt.date}</div>
                  {updatedAt.time && <div className="time-subtext">{updatedAt.time}</div>}
                </div>
                {renderActions(item)}
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}