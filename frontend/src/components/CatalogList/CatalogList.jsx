import { useEffect, useId, useRef, useState } from 'react';
import { MdMoreVert, MdOutlineDelete, MdOutlineEdit, MdOutlineVisibility } from 'react-icons/md';

import { formatDate } from '../../utils/formatDate.js';
import { getUserInitials } from '../../utils/getUserInitials.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './CatalogList.css';

const QUESTION_TYPE_LABELS = {
  TEXT: 'Texto',
  NUMBER: 'Número',
  SINGLE_CHOICE: 'Opción única',
  MULTIPLE_CHOICE: 'Opción múltiple',
  SCALE: 'Escala',
  MATRIX_SCALE: 'Matriz',
  BOOLEAN: 'Sí / No',
};

export function CatalogList({
  items,
  config,
  isLoading,
  errorMessage,
  canManageCatalog,
  itemsPerPage,
  onView,
  onEdit,
  onDelete,
}) {
  const [openRowMenuId, setOpenRowMenuId] = useState(null);
  const actionMenuRef = useRef(null);
  const actionTriggerRef = useRef(null);
  const focusLastActionOnOpenRef = useRef(false);
  const scrollHelpId = `catalog-scroll-help-${useId().replaceAll(':', '')}`;
  const actionMenuBaseId = `catalog-action-menu-${useId().replaceAll(':', '')}`;
  const hasActions = canManageCatalog || Boolean(onView);
  const isScrollable = items.length > 5;

  useEffect(() => {
    if (openRowMenuId === null) return undefined;

    function handleClickOutside(event) {
      if (actionMenuRef.current && !actionMenuRef.current.contains(event.target)) {
        setOpenRowMenuId(null);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [openRowMenuId]);

  useEffect(() => {
    if (openRowMenuId === null) return undefined;

    const menuItems = Array.from(actionMenuRef.current?.querySelectorAll('[role="menuitem"]') ?? []);
    const itemToFocus = focusLastActionOnOpenRef.current
      ? menuItems.at(-1)
      : menuItems[0];
    focusLastActionOnOpenRef.current = false;
    itemToFocus?.focus();

    function handleEscape(event) {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      const trigger = actionTriggerRef.current;
      setOpenRowMenuId(null);
      window.requestAnimationFrame(() => trigger?.focus());
    }

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [openRowMenuId]);

  if (isLoading && items.length === 0) {
    return (
      <Skeleton
        variant="list"
        count={Math.min(itemsPerPage, 5)}
        hasActions={hasActions}
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
      <section className="catalog-list-section catalog-list-section--empty">
        <div className="empty-catalog-card">
          <p>No se encontraron {config.label.toLowerCase()} disponibles.</p>
        </div>
      </section>
    );
  }

  const DefaultItemIcon = config.icon;

  function handleActionMenuKeyDown(event) {
    if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;

    const menuItems = Array.from(event.currentTarget.querySelectorAll('[role="menuitem"]'));
    if (menuItems.length === 0) return;

    event.preventDefault();
    const currentIndex = menuItems.indexOf(document.activeElement);
    let nextIndex;

    if (event.key === 'Home') nextIndex = 0;
    else if (event.key === 'End') nextIndex = menuItems.length - 1;
    else if (event.key === 'ArrowUp') nextIndex = currentIndex <= 0 ? menuItems.length - 1 : currentIndex - 1;
    else nextIndex = currentIndex >= menuItems.length - 1 ? 0 : currentIndex + 1;

    menuItems[nextIndex]?.focus();
  }

  function renderActions(item) {
    if (!canManageCatalog) {
      if (!onView) return null;
      return (
        <div className="catalog-actions-cell catalog-actions-cell--view">
          <button
            type="button"
            className="btn-item-view"
            aria-label={`Ver detalles de ${item[config.titleKey] || config.singularLabel}`}
            onClick={() => onView(item)}
          >
            <MdOutlineVisibility aria-hidden="true" />
            <span>Ver detalles</span>
          </button>
        </div>
      );
    }

    const isOpen = openRowMenuId === item.id;
    const itemLabel = item[config.titleKey] || config.singularLabel;
    const triggerId = `${actionMenuBaseId}-trigger-${item.id}`;
    const menuId = `${actionMenuBaseId}-${item.id}`;

    return (
      <div
        className="catalog-actions-cell"
        onBlurCapture={(event) => {
          if (event.relatedTarget && !event.currentTarget.contains(event.relatedTarget)) {
            setOpenRowMenuId(null);
          }
        }}
      >
        <button
          ref={isOpen ? actionTriggerRef : undefined}
          id={triggerId}
          type="button"
          className="btn-item-more"
          aria-label={`Opciones de ${itemLabel}`}
          aria-haspopup="menu"
          aria-expanded={isOpen}
          aria-controls={isOpen ? menuId : undefined}
          onMouseDown={(event) => event.stopPropagation()}
          onKeyDown={(event) => {
            if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
            event.preventDefault();
            focusLastActionOnOpenRef.current = event.key === 'ArrowUp';
            if (!isOpen) setOpenRowMenuId(item.id);
          }}
          onClick={(event) => {
            event.stopPropagation();
            focusLastActionOnOpenRef.current = false;
            setOpenRowMenuId(isOpen ? null : item.id);
          }}
        >
          <MdMoreVert aria-hidden="true" />
        </button>

        {isOpen && (
          <div
            ref={actionMenuRef}
            id={menuId}
            className="catalog-action-menu"
            role="menu"
            aria-labelledby={triggerId}
            onKeyDown={handleActionMenuKeyDown}
          >
            {onView && (
              <button
                type="button"
                className="menu-option menu-option-details"
                role="menuitem"
                tabIndex={-1}
                onClick={() => {
                  setOpenRowMenuId(null);
                  onView(item);
                }}
              >
                <MdOutlineVisibility aria-hidden="true" />
                <span>Ver detalles</span>
              </button>
            )}
            <button
              type="button"
              className="menu-option"
              role="menuitem"
              tabIndex={-1}
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
              role="menuitem"
              tabIndex={-1}
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

  return (
    <section className="catalog-list-section">
      <div className="catalog-blocks-wrapper">
        <div className={`catalog-list-columns-header ${!hasActions ? 'no-actions' : ''} ${!canManageCatalog && onView ? 'view-action' : ''}`}>
          <span className="col-main-header">{config.singularLabel.toUpperCase()}</span>
          <span className="col-creator-header">CREADOR</span>
          <span className="col-count-header">{config.detailLabel.toUpperCase()}</span>
          <span className="col-created-header">CREADO</span>
          <span className="col-updated-header">ACTUALIZADO</span>
          {hasActions && <span className="col-actions-header">ACCIONES</span>}
        </div>

        {isScrollable && (
          <p id={scrollHelpId} className="catalog-scroll-help">
            Lista desplazable: usa la rueda o las flechas para ver más resultados.
          </p>
        )}

        <div
          className={`catalog-blocks-list ${isScrollable ? 'catalog-items-scroll' : ''}`}
          role={isScrollable ? 'region' : undefined}
          aria-label={isScrollable ? `${config.label}: resultados desplazables` : undefined}
          aria-describedby={isScrollable ? scrollHelpId : undefined}
          tabIndex={isScrollable ? 0 : undefined}
        >
          {items.map((item) => {
            const questionTypeConfig = config.detailKey === 'questionType'
              ? QUESTION_TYPES.find((type) => type.id === item.questionType)
              : null;
            const ItemIcon = questionTypeConfig?.icon ?? DefaultItemIcon;
            const rawTitle = item[config.titleKey] || 'Sin título';
            const title = config.titleKey === 'name' ? rawTitle.toUpperCase() : rawTitle;
            const description = config.descriptionKey
              ? item[config.descriptionKey] || 'Sin descripción.'
              : '';
            const creator = item.creatorName || '';
            const createdAt = formatDate(item.createdAt, { includeTime: true });
            const updatedAt = formatDate(item.updatedAt, { includeTime: true });
            const detailValue =
              config.detailKey === 'questionType'
                ? QUESTION_TYPE_LABELS[item.questionType] ?? item.questionType ?? '—'
                : item[config.detailKey] ?? 0;

            return (
              <article
                key={item.id}
                className={`catalog-block-card ${!hasActions ? 'no-actions' : ''} ${!canManageCatalog && onView ? 'view-action' : ''}`}
              >
                <div className="col-main catalog-item-main">
                  <div
                    className={`catalog-item-icon ${questionTypeConfig ? 'catalog-item-icon--question' : ''}`}
                    style={questionTypeConfig
                      ? { color: questionTypeConfig.color, borderColor: `${questionTypeConfig.color}60` }
                      : undefined}
                    title={questionTypeConfig?.title}
                  >
                    <ItemIcon aria-hidden="true" />
                  </div>
                  <div className="catalog-item-info">
                    <h3>{title}</h3>
                    {description && <p>{description}</p>}
                  </div>
                </div>
                <div className="col-creator user-creator-cell">
                  <span className="user-avatar-circle" aria-hidden="true">
                    {getUserInitials(creator)}
                  </span>
                  <span className="user-creator-name">{creator || '—'}</span>
                </div>
                <div className="col-count font-numeric">{detailValue}</div>
                <div className="col-created font-numeric font-date">
                  <div>{createdAt.date}</div>
                  {createdAt.time && <div className="time-subtext">{createdAt.time}</div>}
                </div>
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
