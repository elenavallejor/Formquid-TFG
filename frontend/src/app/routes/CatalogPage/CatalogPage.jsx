import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import {
  MdAdd,
  MdChevronLeft,
  MdChevronRight,
  MdFormatListBulleted,
  MdGridView,
  MdKeyboardArrowDown,
  MdKeyboardArrowRight,
  MdMoreVert,
  MdOutlineDelete,
  MdOutlineDescription,
  MdOutlineEdit,
  MdOutlineHelpOutline,
  MdOutlineWidgets,
  MdSearch,
} from 'react-icons/md';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlocks,
  getCatalogQuestions,
  getCatalogTemplates,
} from '../../../api/catalogApi.js';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './CatalogPage.css';

const SEARCH_DEBOUNCE_MS = 300;
const DEFAULT_PAGE_SIZE = 10;

const SECTION_CONFIG = {
  plantillas: {
    label: 'Plantillas',
    singularLabel: 'plantilla',
    createPath: '/catalogo/crear/plantilla',
    editPathPrefix: '/catalogo/editar/plantilla',
    icon: MdOutlineDescription,
    load: getCatalogTemplates,
    deleteApi: deleteCatalogTemplate,
    titleKey: 'title',
    descriptionKey: 'description',
    detailLabel: 'PREGUNTAS',
    detailKey: 'questionCount',
  },
  bloques: {
    label: 'Bloques',
    singularLabel: 'bloque',
    createPath: '/catalogo/crear/bloque',
    editPathPrefix: '/catalogo/editar/bloque',
    icon: MdOutlineWidgets,
    load: getCatalogBlocks,
    deleteApi: deleteCatalogBlock,
    titleKey: 'name',
    descriptionKey: 'description',
    detailLabel: 'PREGUNTAS',
    detailKey: 'questionCount',
  },
  preguntas: {
    label: 'Preguntas fijas',
    singularLabel: 'pregunta',
    createPath: '/catalogo/crear/pregunta',
    editPathPrefix: '/catalogo/editar/pregunta',
    icon: MdOutlineHelpOutline,
    load: getCatalogQuestions,
    deleteApi: deleteCatalogQuestion,
    titleKey: 'catalogName',
    descriptionKey: 'statement',
    detailLabel: 'TIPO',
    detailKey: 'questionType',
  },
};

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

function getInitials(name) {
  if (!name) return '—';
  return name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();
}

function formatUpdatedAt(value) {
  if (!value) {
    return { date: '—', time: '' };
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return { date: '—', time: '' };
  }
  return {
    date: date.toLocaleDateString('es-ES'),
    time: date.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
    }),
  };
}

function getVisiblePages(currentPage, totalPages) {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }
  const firstPage = Math.min(
    Math.max(currentPage - 2, 0),
    totalPages - 5,
  );
  return Array.from({ length: 5 }, (_, index) => firstPage + index);
}

export function CatalogPage() {
  const navigate = useNavigate();
  const { section } = useParams();
  const { user } = useAuth();
  const requestVersionRef = useRef(0);
  const actionMenuRef = useRef(null);
  const createMenuRef = useRef(null);
  const config = SECTION_CONFIG[section];

  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [viewMode, setViewMode] = useState('LIST');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [openRowMenuId, setOpenRowMenuId] = useState(null);
  const [deleteModalState, setDeleteModalState] = useState({
    isOpen: false,
    item: null,
  });

  const canManageCatalog =
    user?.role === 'ADMIN' ||
    user?.role === 'ADMINISTRADOR' ||
    user?.role === 'SUPERADMIN';

  useEffect(() => {
    setCurrentPage(0);
    setOpenRowMenuId(null);
  }, [section]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeoutId);
  }, [searchQuery]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (
        createMenuRef.current &&
        !createMenuRef.current.contains(event.target)
      ) {
        setIsCreateMenuOpen(false);
      }
      if (
        actionMenuRef.current &&
        !actionMenuRef.current.contains(event.target)
      ) {
        setOpenRowMenuId(null);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (!config) return undefined;

    const requestVersion = ++requestVersionRef.current;

    async function loadCatalog() {
      try {
        setIsLoading(true);
        setErrorMessage('');
        const response = await config.load({
          search: debouncedSearchQuery,
          category: categoryFilter,
          sortOrder,
          page: currentPage,
          size: itemsPerPage,
        });

        if (requestVersion !== requestVersionRef.current) return;

        setItems(Array.isArray(response?.content) ? response.content : []);
        setTotalItems(Number(response?.totalElements ?? 0));
        setTotalPages(Number(response?.totalPages ?? 0));
      } catch (error) {
        if (requestVersion !== requestVersionRef.current) return;
        console.error('Error al cargar el catálogo:', error);
        setItems([]);
        setTotalItems(0);
        setTotalPages(0);
        setErrorMessage('No se pudieron cargar los elementos del catálogo.');
      } finally {
        if (requestVersion === requestVersionRef.current) {
          setIsLoading(false);
        }
      }
    }

    void loadCatalog();
    return undefined;
  }, [
    config,
    debouncedSearchQuery,
    categoryFilter,
    sortOrder,
    currentPage,
    itemsPerPage,
  ]);

  const visiblePages = useMemo(
    () => getVisiblePages(currentPage, totalPages),
    [currentPage, totalPages],
  );

  if (!config) {
    return <Navigate to="/catalogo/bloques" replace />;
  }

  const handleDeleteConfirm = async () => {
    const itemToDelete = deleteModalState.item;
    if (!itemToDelete) return;

    try {
      await config.deleteApi(itemToDelete.id);
      setItems((prev) => prev.filter((i) => i.id !== itemToDelete.id));
      setTotalItems((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Error al eliminar elemento:', error);
    } finally {
      setDeleteModalState({ isOpen: false, item: null });
      setOpenRowMenuId(null);
    }
  };

  const startItem = totalItems === 0 ? 0 : currentPage * itemsPerPage + 1;
  const endItem = Math.min((currentPage + 1) * itemsPerPage, totalItems);
  const ItemIcon = config.icon;

  return (
    <main className="catalog-page">
      <div className="catalog-container">
        <nav className="breadcrumb-nav" aria-label="Migas de pan">
          <Link to="/panel">PANEL</Link>
          <MdKeyboardArrowRight aria-hidden="true" />
          <span aria-current="page">CATÁLOGO</span>
        </nav>

        <header className="catalog-page-header">
          <h1>CATÁLOGO</h1>
          {canManageCatalog && (
            <div className="create-element-dropdown-container" ref={createMenuRef}>
              <button
                type="button"
                className="btn-catalog-create-dropdown"
                onClick={() => setIsCreateMenuOpen((prev) => !prev)}
              >
                <MdAdd className="btn-create-icon" aria-hidden="true" />
                <span>Crear elemento</span>
                <MdKeyboardArrowDown className="btn-dropdown-arrow" aria-hidden="true" />
              </button>

              {isCreateMenuOpen && (
                <div className="create-element-menu">
                  <Link
                    to="/catalogo/crear/plantilla"
                    onClick={() => setIsCreateMenuOpen(false)}
                  >
                    <MdOutlineDescription aria-hidden="true" />
                    <span>Plantilla</span>
                  </Link>
                  <Link
                    to="/catalogo/crear/bloque"
                    onClick={() => setIsCreateMenuOpen(false)}
                  >
                    <MdOutlineWidgets aria-hidden="true" />
                    <span>Bloque</span>
                  </Link>
                  <Link
                    to="/catalogo/crear/pregunta"
                    onClick={() => setIsCreateMenuOpen(false)}
                  >
                    <MdOutlineHelpOutline aria-hidden="true" />
                    <span>Pregunta fija</span>
                  </Link>
                </div>
              )}
            </div>
          )}
        </header>

        <div className="catalog-tabs-container" role="tablist" aria-label="Tipo de elemento">
          {Object.entries(SECTION_CONFIG).map(([sectionKey, tab]) => {
            const TabIcon = tab.icon;
            const isActive = section === sectionKey;

            return (
              <Link
                key={sectionKey}
                to={`/catalogo/${sectionKey}`}
                className={`catalog-tab ${isActive ? 'active' : ''}`}
                role="tab"
                aria-selected={isActive}
              >
                <TabIcon className="tab-icon" aria-hidden="true" />
                <span>{tab.label}</span>
              </Link>
            );
          })}
        </div>

        <section className="catalog-filters-bar" aria-label="Filtros del catálogo">
          <div className="search-input-wrapper">
            <MdSearch className="search-icon" aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre, descripción o creador..."
              value={searchQuery}
              aria-label={`Buscar ${config.label.toLowerCase()}`}
              onChange={(event) => {
                setSearchQuery(event.target.value);
                setCurrentPage(0);
              }}
            />
          </div>

          <div className="catalog-filters-right">
            <div className="category-select-wrapper">
              <select
                value={categoryFilter}
                aria-label="Filtrar por categoría"
                onChange={(event) => {
                  setCategoryFilter(event.target.value);
                  setCurrentPage(0);
                }}
              >
                <option value="ALL">Todas las categorías</option>
                <option value="Demográfico">Demográfico</option>
                <option value="Satisfacción">Satisfacción</option>
                <option value="Feedback">Feedback</option>
                <option value="General">General</option>
              </select>
            </div>

            <div className="sort-select-wrapper">
              <span className="sort-label">Ordenar por:</span>
              <select
                value={sortOrder}
                aria-label="Ordenar catálogo"
                onChange={(event) => {
                  setSortOrder(event.target.value);
                  setCurrentPage(0);
                }}
              >
                <option value="NEWEST">Más reciente</option>
                <option value="OLDEST">Más antigua</option>
              </select>
            </div>

            <div className="view-mode-toggle">
              <button
                type="button"
                className={`toggle-btn ${viewMode === 'GRID' ? 'active' : ''}`}
                aria-label="Vista en cuadrícula"
                onClick={() => setViewMode('GRID')}
              >
                <MdGridView aria-hidden="true" />
              </button>
              <button
                type="button"
                className={`toggle-btn ${viewMode === 'LIST' ? 'active' : ''}`}
                aria-label="Vista en lista"
                onClick={() => setViewMode('LIST')}
              >
                <MdFormatListBulleted aria-hidden="true" />
              </button>
            </div>
          </div>
        </section>

        <section className="catalog-list-section" aria-busy={isLoading}>
          {isLoading && items.length === 0 ? (
            <p className="catalog-status-text" aria-live="polite">
              Cargando elementos del catálogo...
            </p>
          ) : errorMessage ? (
            <p className="catalog-error-text" role="alert">
              {errorMessage}
            </p>
          ) : items.length === 0 ? (
            <div className="empty-catalog-card">
              <p>No se encontraron {config.label.toLowerCase()} disponibles.</p>
            </div>
          ) : viewMode === 'GRID' ? (
            <div className="catalog-grid-body">
              {items.map((item) => {
                const title = item[config.titleKey] || 'Sin título';
                const description = item[config.descriptionKey] || 'Sin descripción.';
                const creator = item.creatorName || '—';
                const updatedAt = formatUpdatedAt(item.updatedAt);
                const detailValue =
                  config.detailKey === 'questionType'
                    ? QUESTION_TYPE_LABELS[item.questionType] ?? item.questionType ?? '—'
                    : item[config.detailKey] ?? 0;

                return (
                  <article key={item.id} className="catalog-grid-card">
                    <div className="card-top">
                      <div className="catalog-item-icon">
                        <ItemIcon aria-hidden="true" />
                      </div>
                      {canManageCatalog && (
                        <div className="catalog-actions-cell">
                          <button
                            type="button"
                            className="btn-item-more"
                            aria-label="Opciones"
                            onClick={() =>
                              setOpenRowMenuId(openRowMenuId === item.id ? null : item.id)
                            }
                          >
                            <MdMoreVert aria-hidden="true" />
                          </button>

                          {openRowMenuId === item.id && (
                            <div className="catalog-action-menu" ref={actionMenuRef}>
                              <button
                                type="button"
                                className="menu-option"
                                onClick={() => navigate(`${config.editPathPrefix}/${item.id}`)}
                              >
                                <MdOutlineEdit aria-hidden="true" />
                                <span>Editar</span>
                              </button>
                              <button
                                type="button"
                                className="menu-option option-delete"
                                onClick={() => setDeleteModalState({ isOpen: true, item })}
                              >
                                <MdOutlineDelete aria-hidden="true" />
                                <span>Borrar</span>
                              </button>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    <div className="card-body">
                      <h3>{title}</h3>
                      <p>{description}</p>
                    </div>

                    <div className="card-footer">
                      <div className="user-creator-cell">
                        <span className="user-avatar-circle" aria-hidden="true">
                          {getInitials(creator)}
                        </span>
                        <span className="user-creator-name">{creator}</span>
                      </div>
                      <div className="card-metrics font-numeric">
                        <span>{detailValue}</span>
                        <span>{updatedAt.date}</span>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          ) : (
            <>
              <div
                className={`catalog-table-header ${
                  !canManageCatalog ? 'no-actions' : ''
                }`}
              >
                <span className="col-title">{config.singularLabel.toUpperCase()}</span>
                <span className="col-creator">CREADOR</span>
                <span className="col-count">{config.detailLabel}</span>
                <span className="col-updated">ACTUALIZADO</span>
                {canManageCatalog && <span className="col-actions">ACCIONES</span>}
              </div>

              <div className="catalog-table-body">
                {items.map((item) => {
                  const title = item[config.titleKey] || 'Sin título';
                  const description = item[config.descriptionKey] || 'Sin descripción.';
                  const creator = item.creatorName || '—';
                  const updatedAt = formatUpdatedAt(item.updatedAt);
                  const detailValue =
                    config.detailKey === 'questionType'
                      ? QUESTION_TYPE_LABELS[item.questionType] ?? item.questionType ?? '—'
                      : item[config.detailKey] ?? 0;

                  return (
                    <article
                      key={item.id}
                      className={`catalog-item-row ${
                        !canManageCatalog ? 'no-actions' : ''
                      }`}
                    >
                      <div className="col-title catalog-item-main">
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
                          {getInitials(creator)}
                        </span>
                        <span className="user-creator-name">{creator}</span>
                      </div>

                      <div className="col-count font-numeric">{detailValue}</div>

                      <div className="col-updated font-numeric font-date">
                        <div>{updatedAt.date}</div>
                        {updatedAt.time && (
                          <div className="time-subtext">{updatedAt.time}</div>
                        )}
                      </div>

                      {canManageCatalog && (
                        <div className="col-actions catalog-actions-cell">
                          <button
                            type="button"
                            className="btn-item-more"
                            aria-label="Opciones"
                            onClick={() =>
                              setOpenRowMenuId(openRowMenuId === item.id ? null : item.id)
                            }
                          >
                            <MdMoreVert aria-hidden="true" />
                          </button>

                          {openRowMenuId === item.id && (
                            <div className="catalog-action-menu" ref={actionMenuRef}>
                              <button
                                type="button"
                                className="menu-option"
                                onClick={() => navigate(`${config.editPathPrefix}/${item.id}`)}
                              >
                                <MdOutlineEdit aria-hidden="true" />
                                <span>Editar</span>
                              </button>
                              <button
                                type="button"
                                className="menu-option option-delete"
                                onClick={() => setDeleteModalState({ isOpen: true, item })}
                              >
                                <MdOutlineDelete aria-hidden="true" />
                                <span>Borrar</span>
                              </button>
                            </div>
                          )}
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            </>
          )}
        </section>

        {!isLoading && !errorMessage && totalItems > 0 && (
          <footer className="catalog-pagination-footer">
            <div className="pagination-count-label">
              Mostrando {startItem} a {endItem} de {totalItems} {config.label.toLowerCase()}
            </div>

            <div className="pagination-numbers-bar" aria-label="Paginación del catálogo">
              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage === 0}
                aria-label="Página anterior"
                onClick={() => setCurrentPage((page) => Math.max(page - 1, 0))}
              >
                <MdChevronLeft aria-hidden="true" />
              </button>

              {visiblePages.map((page) => (
                <button
                  key={page}
                  type="button"
                  className={`btn-page-number ${currentPage === page ? 'active' : ''}`}
                  aria-current={currentPage === page ? 'page' : undefined}
                  onClick={() => setCurrentPage(page)}
                >
                  {page + 1}
                </button>
              ))}

              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage >= totalPages - 1}
                aria-label="Página siguiente"
                onClick={() => setCurrentPage((page) => Math.min(page + 1, totalPages - 1))}
              >
                <MdChevronRight aria-hidden="true" />
              </button>
            </div>

            <div className="pagination-per-page">
              <select
                value={itemsPerPage}
                aria-label="Elementos por página"
                onChange={(event) => {
                  setItemsPerPage(Number(event.target.value));
                  setCurrentPage(0);
                }}
              >
                <option value={5}>5 por página</option>
                <option value={10}>10 por página</option>
                <option value={20}>20 por página</option>
              </select>
            </div>
          </footer>
        )}
      </div>

      <ConfirmModal
        isOpen={deleteModalState.isOpen}
        onClose={() => setDeleteModalState({ isOpen: false, item: null })}
        onConfirm={handleDeleteConfirm}
        actionType="DENY"
        applicantName={
          deleteModalState.item?.[config.titleKey] || 'elemento del catálogo'
        }
      />
    </main>
  );
}