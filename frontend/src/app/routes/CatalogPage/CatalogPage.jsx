import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { MdAdd, MdKeyboardArrowDown, MdKeyboardArrowRight, MdOutlineDescription, MdOutlineHelpOutline, MdOutlineWidgets, MdSearch } from 'react-icons/md';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlocks,
  getCatalogQuestions,
  getCatalogTemplates,
} from '../../../api/catalogApi.js';
import { CatalogList } from '../../../components/CatalogList/CatalogList.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './CatalogPage.css';

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [5, 10, 20];

const SECTION_CONFIG = {
  plantillas: {
    label: 'Plantillas',
    ownLabel: 'MIS PLANTILLAS',
    singularLabel: 'plantilla',
    elementType: 'plantilla',
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
    ownLabel: 'MIS BLOQUES',
    singularLabel: 'bloque',
    elementType: 'bloque',
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
    ownLabel: 'MIS PREGUNTAS',
    singularLabel: 'pregunta',
    elementType: 'pregunta',
    editPathPrefix: '/catalogo/editar/pregunta',
    icon: MdOutlineHelpOutline,
    load: getCatalogQuestions,
    deleteApi: deleteCatalogQuestion,
    titleKey: 'statement',
    descriptionKey: null,
    detailLabel: 'tipo',
    detailKey: 'questionType',
  },
};

function emptyCatalogState(isLoading = true) {
  return {
    items: [],
    totalItems: 0,
    totalPages: 0,
    isLoading,
    errorMessage: '',
  };
}

function filterAndSortItems(items, config, searchQuery, sortOrder) {
  if (!config) return [];
  const query = searchQuery.trim().toLowerCase();

  return items
    .filter((item) => {
      if (!query) return true;
      const title = String(item[config.titleKey] ?? '').toLowerCase();
      const description = config.descriptionKey
        ? String(item[config.descriptionKey] ?? '').toLowerCase()
        : '';
      const creator = String(item.creatorName ?? '').toLowerCase();
      return title.includes(query) || description.includes(query) || creator.includes(query);
    })
    .sort((first, second) => {
      const firstDate = new Date(first.updatedAt ?? 0).getTime();
      const secondDate = new Date(second.updatedAt ?? 0).getTime();
      return sortOrder === 'OLDEST' ? firstDate - secondDate : secondDate - firstDate;
    });
}

export function CatalogPage() {
  const navigate = useNavigate();
  const { section } = useParams();
  const { user } = useAuth();
  const config = SECTION_CONFIG[section];

  const requestVersionRef = useRef(0);
  const createMenuRef = useRef(null);

  const [ownCatalog, setOwnCatalog] = useState(() => emptyCatalogState());
  const [publicCatalog, setPublicCatalog] = useState(() => emptyCatalogState());
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [ownCurrentPage, setOwnCurrentPage] = useState(0);
  const [publicCurrentPage, setPublicCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [deleteModalState, setDeleteModalState] = useState({ isOpen: false, item: null, source: null });

  const canCreateCatalog = user?.role === 'ADMIN' || user?.role === 'SUPERADMIN';
  const isSuperadmin = user?.role === 'SUPERADMIN';

  useEffect(() => {
    setOwnCurrentPage(0);
    setPublicCurrentPage(0);
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
    if (!config || !user?.id) return undefined;

    const requestVersion = ++requestVersionRef.current;

    async function loadCatalog(mine, page, setCatalog) {
      setCatalog(emptyCatalogState());
      try {
        const response = await config.load({
          page,
          size: itemsPerPage,
          mine,
        });

        if (requestVersion !== requestVersionRef.current) return;

        setCatalog({
          items: Array.isArray(response?.content) ? response.content : [],
          totalItems: Number(response?.totalElements ?? 0),
          totalPages: Number(response?.totalPages ?? 0),
          isLoading: false,
          errorMessage: '',
        });
      } catch (error) {
        if (requestVersion !== requestVersionRef.current) return;
        console.error('Error al cargar el catálogo:', error);
        setCatalog({
          ...emptyCatalogState(false),
          errorMessage: 'No se pudieron cargar los elementos del catálogo.',
        });
      }
    }

    if (canCreateCatalog) {
      void loadCatalog(true, ownCurrentPage, setOwnCatalog);
    } else {
      setOwnCatalog(emptyCatalogState(false));
    }
    void loadCatalog(false, publicCurrentPage, setPublicCatalog);
    return undefined;
  }, [canCreateCatalog, config, itemsPerPage, ownCurrentPage, publicCurrentPage, user?.id]);

  const visibleOwnItems = filterAndSortItems(ownCatalog.items, config, searchQuery, sortOrder);
  const visiblePublicItems = filterAndSortItems(publicCatalog.items, config, searchQuery, sortOrder);

  if (!config) {
    return <Navigate to="/catalogo/bloques" replace />;
  }

  async function handleDeleteConfirm() {
    const { item: itemToDelete, source } = deleteModalState;
    if (!itemToDelete) return;

    try {
      await config.deleteApi(itemToDelete.id);
      const removeItem = (current) => ({
        ...current,
        items: current.items.filter((item) => item.id !== itemToDelete.id),
        totalItems: Math.max(0, current.totalItems - 1),
      });
      if (source === 'own') setOwnCatalog(removeItem);
      else setPublicCatalog(removeItem);
    } catch (error) {
      console.error('Error al eliminar elemento:', error);
    } finally {
      setDeleteModalState({ isOpen: false, item: null, source: null });
    }
  }

  function handlePageSizeChange(value) {
    setItemsPerPage(value);
    setOwnCurrentPage(0);
    setPublicCurrentPage(0);
  }

  function renderPagination(catalog, currentPage, setCurrentPage, source) {
    if (catalog.isLoading || catalog.errorMessage || catalog.totalItems === 0) return null;
    const paginationName = source === 'own'
      ? `mis ${config.label.toLowerCase()}`
      : `${config.label.toLowerCase()} de la lista pública`;

    return (
      <Pagination
        ariaLabel={`Paginación de ${paginationName}`}
        currentPage={currentPage}
        itemLabel={config.label.toLowerCase()}
        onPageChange={setCurrentPage}
        onPageSizeChange={handlePageSizeChange}
        pageSize={itemsPerPage}
        pageSizeLabel={`Elementos por página en ${paginationName}`}
        pageSizeOptions={PAGE_SIZE_OPTIONS}
        totalItems={catalog.totalItems}
        totalPages={catalog.totalPages}
      />
    );
  }

  function renderCatalogSection({
    title,
    catalog,
    visibleItems,
    currentPage,
    setCurrentPage,
    canManage,
    source,
  }) {
    return (
      <section className="catalog-results-section" aria-labelledby={`catalog-${source}-title`}>
        <header className="catalog-results-header">
          <h2 id={`catalog-${source}-title`}>{title}</h2>
        </header>

        <CatalogList
          items={visibleItems}
          config={config}
          isLoading={catalog.isLoading}
          errorMessage={catalog.errorMessage}
          canManageCatalog={canManage}
          itemsPerPage={itemsPerPage}
          onView={(item) => navigate(`/catalogo/detalles/${config.elementType}/${item.id}`)}
          onEdit={(id) => navigate(`${config.editPathPrefix}/${id}`)}
          onDelete={(item) => setDeleteModalState({ isOpen: true, item, source })}
        />

        {renderPagination(catalog, currentPage, setCurrentPage, source)}
      </section>
    );
  }

  return (
    <main className="catalog-page">
      <div className="catalog-container">
        <Breadcrumbs
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'CATÁLOGO' },
          ]}
          separator={MdKeyboardArrowRight}
        />

        <header className="catalog-page-header">
          <h1>CATÁLOGO</h1>
          {canCreateCatalog && (
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
              <Dropdown
                aria-label="Ordenar catálogo"
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              >
                <option value="NEWEST">Más reciente</option>
                <option value="OLDEST">Más antigua</option>
              </Dropdown>
            </div>
          </div>
        </section>

        <div className="catalog-results">
          {canCreateCatalog && renderCatalogSection({
            title: config.ownLabel,
            catalog: ownCatalog,
            visibleItems: visibleOwnItems,
            currentPage: ownCurrentPage,
            setCurrentPage: setOwnCurrentPage,
            canManage: true,
            source: 'own',
          })}

          {renderCatalogSection({
            title: 'LISTA PÚBLICA',
            catalog: publicCatalog,
            visibleItems: visiblePublicItems,
            currentPage: publicCurrentPage,
            setCurrentPage: setPublicCurrentPage,
            canManage: isSuperadmin,
            source: 'public',
          })}
        </div>
      </div>

      <ConfirmModal
        isOpen={deleteModalState.isOpen}
        onClose={() => setDeleteModalState({ isOpen: false, item: null, source: null })}
        onConfirm={handleDeleteConfirm}
        title={`Eliminar ${config.singularLabel}`}
        message={`¿Confirmas que deseas eliminar “${deleteModalState.item?.[config.titleKey] || 'este elemento'}”? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar"
        variant="danger"
      />

    </main>
  );
}
