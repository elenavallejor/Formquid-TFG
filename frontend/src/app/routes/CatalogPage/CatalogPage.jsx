import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { MdAdd, MdChevronLeft, MdChevronRight, MdFormatListBulleted, MdGridView, MdKeyboardArrowDown, MdKeyboardArrowRight, MdOutlineDescription, MdOutlineHelpOutline, MdOutlineWidgets, MdSearch } from 'react-icons/md';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlocks,
  getCatalogQuestions,
  getCatalogTemplates,
} from '../../../api/catalogApi.js';
import { CatalogList } from '../../../components/CatalogList/CatalogList.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './CatalogPage.css';

const DEFAULT_PAGE_SIZE = 10;

const SECTION_CONFIG = {
  plantillas: {
    label: 'Plantillas',
    singularLabel: 'plantilla',
    editPathPrefix: '/catalogo/editar/plantilla',
    icon: MdOutlineDescription,
    load: getCatalogTemplates,
    deleteApi: deleteCatalogTemplate,
    titleKey: 'title',
    descriptionKey: 'description',
    detailLabel: 'preguntas',
    detailKey: 'questionCount',
  },
  bloques: {
    label: 'Bloques',
    singularLabel: 'bloque',
    editPathPrefix: '/catalogo/editar/bloque',
    icon: MdOutlineWidgets,
    load: getCatalogBlocks,
    deleteApi: deleteCatalogBlock,
    titleKey: 'name',
    descriptionKey: 'description',
    detailLabel: 'preguntas',
    detailKey: 'questionCount',
  },
  preguntas: {
    label: 'Preguntas fijas',
    singularLabel: 'pregunta',
    editPathPrefix: '/catalogo/editar/pregunta',
    icon: MdOutlineHelpOutline,
    load: getCatalogQuestions,
    deleteApi: deleteCatalogQuestion,
    titleKey: 'catalogName',
    descriptionKey: 'statement',
    detailLabel: 'tipo',
    detailKey: 'questionType',
  },
};

function getVisiblePages(currentPage, totalPages) {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }
  const firstPage = Math.min(Math.max(currentPage - 2, 0), totalPages - 5);
  return Array.from({ length: 5 }, (_, index) => firstPage + index);
}

export function CatalogPage() {
  const navigate = useNavigate();
  const { section } = useParams();
  const { user } = useAuth();
  const config = SECTION_CONFIG[section];

  const requestVersionRef = useRef(0);
  const createMenuRef = useRef(null);

  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [viewMode, setViewMode] = useState('LIST');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  
  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [deleteModalState, setDeleteModalState] = useState({ isOpen: false, item: null });

  const canManageCatalog = user?.role === 'ADMIN' || user?.role === 'SUPERADMIN';

  useEffect(() => {
    setCurrentPage(0);
    setSearchQuery('');
  }, [section]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (createMenuRef.current && !createMenuRef.current.contains(event.target)) {
        setIsCreateMenuOpen(false);
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
        setItems([]);

        const response = await config.load({
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
        if (requestVersion === requestVersionRef.current) setIsLoading(false);
      }
    }

    void loadCatalog();
    return undefined;
  }, [config, currentPage, itemsPerPage]);

  const visibleItems = useMemo(() => {
    if (!config) return [];
    const query = searchQuery.trim().toLowerCase();

    return items
      .filter((item) => {
        if (!query) return true;
        const title = String(item[config.titleKey] ?? '').toLowerCase();
        const description = String(item[config.descriptionKey] ?? '').toLowerCase();
        const creator = String(item.creatorName ?? '').toLowerCase();
        return title.includes(query) || description.includes(query) || creator.includes(query);
      })
      .sort((first, second) => {
        const firstDate = new Date(first.updatedAt ?? 0).getTime();
        const secondDate = new Date(second.updatedAt ?? 0).getTime();
        return sortOrder === 'OLDEST' ? firstDate - secondDate : secondDate - firstDate;
      });
  }, [items, searchQuery, sortOrder, config]);

  const visiblePages = useMemo(
    () => getVisiblePages(currentPage, totalPages),
    [currentPage, totalPages]
  );

  if (!config) {
    return <Navigate to="/catalogo/bloques" replace />;
  }

  async function handleDeleteConfirm() {
    const itemToDelete = deleteModalState.item;
    if (!itemToDelete) return;

    try {
      await config.deleteApi(itemToDelete.id);
      setItems((prev) => prev.filter((item) => item.id !== itemToDelete.id));
      setTotalItems((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Error al eliminar elemento:', error);
    } finally {
      setDeleteModalState({ isOpen: false, item: null });
    }
  }

  const startItem = totalItems === 0 ? 0 : currentPage * itemsPerPage + 1;
  const endItem = Math.min((currentPage + 1) * itemsPerPage, totalItems);

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
                onClick={() => setIsCreateMenuOpen((current) => !current)}
              >
                <MdAdd className="btn-create-icon" aria-hidden="true" />
                <span>Crear elemento</span>
                <MdKeyboardArrowDown className="btn-dropdown-arrow" aria-hidden="true" />
              </button>
              {isCreateMenuOpen && (
                <div className="create-element-menu">
                  <Link to="/catalogo/crear/plantilla" onClick={() => setIsCreateMenuOpen(false)}>
                    <MdOutlineDescription aria-hidden="true" />
                    <span>Plantilla</span>
                  </Link>
                  <Link to="/catalogo/crear/bloque" onClick={() => setIsCreateMenuOpen(false)}>
                    <MdOutlineWidgets aria-hidden="true" />
                    <span>Bloque</span>
                  </Link>
                  <Link to="/catalogo/crear/pregunta" onClick={() => setIsCreateMenuOpen(false)}>
                    <MdOutlineHelpOutline aria-hidden="true" />
                    <span>Pregunta fija</span>
                  </Link>
                </div>
              )}
            </div>
          )}
        </header>

        <div className="catalog-tabs-container" role="tablist">
          {Object.entries(SECTION_CONFIG).map(([sectionKey, tab]) => (
            <Link
              key={sectionKey}
              to={`/catalogo/${sectionKey}`}
              className={`catalog-tab ${section === sectionKey ? 'active' : ''}`}
              role="tab"
              aria-selected={section === sectionKey}
            >
              <tab.icon className="tab-icon" aria-hidden="true" />
              <span>{tab.label}</span>
            </Link>
          ))}
        </div>

        <section className="catalog-filters-bar" aria-label="Filtros del catálogo">
          <div className="search-input-wrapper">
            <MdSearch className="search-icon" aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre, descripción o creador..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="catalog-filters-right">
            <div className="sort-select-wrapper">
              <span className="sort-label">Ordenar por:</span>
              <select
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              >
                <option value="NEWEST">Más reciente</option>
                <option value="OLDEST">Más antigua</option>
              </select>
            </div>
            <div className="view-mode-toggle">
              <button
                type="button"
                className={`toggle-btn ${viewMode === 'GRID' ? 'active' : ''}`}
                onClick={() => setViewMode('GRID')}
              >
                <MdGridView aria-hidden="true" />
              </button>
              <button
                type="button"
                className={`toggle-btn ${viewMode === 'LIST' ? 'active' : ''}`}
                onClick={() => setViewMode('LIST')}
              >
                <MdFormatListBulleted aria-hidden="true" />
              </button>
            </div>
          </div>
        </section>

        <CatalogList
          items={visibleItems}
          config={config}
          isLoading={isLoading}
          errorMessage={errorMessage}
          viewMode={viewMode}
          canManageCatalog={canManageCatalog}
          itemsPerPage={itemsPerPage}
          onEdit={(id) => navigate(`${config.editPathPrefix}/${id}`)}
          onDelete={(item) => setDeleteModalState({ isOpen: true, item })}
        />

        {!isLoading && !errorMessage && (totalItems > 0 || totalPages > 0) && (
          <footer className="catalog-pagination-footer">
            <div className="pagination-count-label">
              Mostrando {startItem} a {endItem} de {totalItems} {config.label.toLowerCase()}
            </div>

            <div className="pagination-numbers-bar">
              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage === 0 || isLoading}
                onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
              >
                <MdChevronLeft aria-hidden="true" />
              </button>

              {visiblePages.map((page) => (
                <button
                  key={page}
                  type="button"
                  className={`btn-page-number ${currentPage === page ? 'active' : ''}`}
                  disabled={isLoading}
                  onClick={() => setCurrentPage(page)}
                >
                  {page + 1}
                </button>
              ))}

              <button
                type="button"
                className="btn-page-nav"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages - 1))}
              >
                <MdChevronRight aria-hidden="true" />
              </button>
            </div>

            <div className="pagination-per-page">
              <select
                value={itemsPerPage}
                disabled={isLoading}
                onChange={(e) => {
                  setItemsPerPage(Number(e.target.value));
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
        applicantName={deleteModalState.item?.[config.titleKey] || 'elemento del catálogo'}
      />
    </main>
  );
}