import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { MdAdd, MdKeyboardArrowDown, MdKeyboardArrowRight, MdOutlineDescription, MdOutlineHelpOutline, MdOutlineWidgets } from 'react-icons/md';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  deleteCatalogTemplate,
  getCatalogBlocks,
  getCatalogQuestions,
  getCatalogTemplates,
} from '../../../api/catalogApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import { CatalogList } from '../../../components/CatalogList/CatalogList.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ConfirmModal } from '../../../components/ConfirmModal/ConfirmModal.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { useAuth } from '../../../context/useAuth.js';

import { useResultsFocus } from '../../../hooks/useResultsFocus.js';

import './CatalogPage.css';

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [5, 10, 20];
const SEARCH_FIELDS = ['TITLE', 'DESCRIPTION', 'CREATOR'];
const SORT_OPTIONS = [
  { value: 'NEWEST', label: 'Más reciente' },
  { value: 'OLDEST', label: 'Más antigua' },
  { value: 'ALPHABETICAL', label: 'Alfabético (A–Z)' },
];

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

export function CatalogPage() {
  const navigate = useNavigate();
  const { section } = useParams();
  const { user } = useAuth();
  const config = SECTION_CONFIG[section];

  const requestVersionRef = useRef(0);
  const createMenuRef = useRef(null);

  const [ownCatalog, setOwnCatalog] = useState(() => emptyCatalogState());
  const [publicCatalog, setPublicCatalog] = useState(() => emptyCatalogState());
  const { resultsRef: ownResultsRef, requestResultsFocus: requestOwnResultsFocus } = useResultsFocus(ownCatalog.isLoading);
  const { resultsRef: publicResultsRef, requestResultsFocus: requestPublicResultsFocus } = useResultsFocus(publicCatalog.isLoading);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [categories, setCategories] = useState([]);
  const [ownCurrentPage, setOwnCurrentPage] = useState(0);
  const [publicCurrentPage, setPublicCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [deleteModalState, setDeleteModalState] = useState({ isOpen: false, item: null, source: null });

  const canCreateCatalog = user?.role === 'ADMIN' || user?.role === 'SUPERADMIN';
  const isSuperadmin = user?.role === 'SUPERADMIN';

  useEffect(() => {
    let active = true;
    getCategories()
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? response : []);
      })
      .catch(() => {});
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(searchQuery.trim());
    }, 300);

    return () => window.clearTimeout(timeoutId);
  }, [searchQuery]);

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
          search: debouncedSearch,
          searchFields: SEARCH_FIELDS,
          category: selectedCategory || undefined,
          sortOrder,
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
    }
    void loadCatalog(false, publicCurrentPage, setPublicCatalog);
    return undefined;
  }, [
    canCreateCatalog,
    config,
    debouncedSearch,
    itemsPerPage,
    ownCurrentPage,
    publicCurrentPage,
    selectedCategory,
    sortOrder,
    user?.id,
  ]);

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
        onNavigate={source === 'own' ? requestOwnResultsFocus : requestPublicResultsFocus}
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
          <h2 ref={source === 'own' ? ownResultsRef : publicResultsRef} tabIndex={-1} id={`catalog-${source}-title`}>{title}</h2>
        </header>

        <CatalogList
          items={visibleItems}
          categories={categories}
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
              onClick={() => {
                setOwnCurrentPage(0);
                setPublicCurrentPage(0);
              }}
            >
              <tab.icon className="tab-icon" aria-hidden="true" />
              <span>{tab.label}</span>
            </Link>
          ))}
        </div>

        <SearchToolbar
          ariaLabel="Filtros del catálogo"
          searchLabel="Introduce una palabra para buscar en el catálogo"
          searchValue={searchQuery}
          onSearchChange={(event) => {
            setSearchQuery(event.target.value);
            setOwnCurrentPage(0);
            setPublicCurrentPage(0);
          }}
          filterChoice={{
            label: 'Categoría',
            control: 'select',
            options: [
              { value: '', label: 'Todas las categorías' },
              ...categories.map(({ code, name }) => ({ value: code, label: name })),
            ],
            value: selectedCategory,
            resetValue: '',
          }}
          onFilterChange={(_values, _text, category) => {
            setSelectedCategory(category);
            setOwnCurrentPage(0);
            setPublicCurrentPage(0);
          }}
          sortAriaLabel="Ordenar catálogo"
          sortOptions={SORT_OPTIONS}
          sortValue={sortOrder}
          onSortChange={(event) => {
            setSortOrder(event.target.value);
            setOwnCurrentPage(0);
            setPublicCurrentPage(0);
          }}
        />

        <div className="catalog-results">
          {canCreateCatalog && renderCatalogSection({
            title: config.ownLabel,
            catalog: ownCatalog,
            visibleItems: ownCatalog.items,
            currentPage: ownCurrentPage,
            setCurrentPage: setOwnCurrentPage,
            canManage: true,
            source: 'own',
          })}

          {renderCatalogSection({
            title: 'LISTA PÚBLICA',
            catalog: publicCatalog,
            visibleItems: publicCatalog.items,
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
