import { useEffect, useState } from 'react';
import { FaRegClipboard } from 'react-icons/fa';
import {
  MdLockOutline,
  MdOutlinePerson,
  MdPublic,
  MdSearch,
} from 'react-icons/md';

import { getCategories } from '../../../api/categoriesApi.js';
import { getPublicSurveys, getRegisteredSurveys } from '../../../api/surveysApi.js';
import bannerSmall from '../../../assets/images/banner.png';
import { Filter } from '../../../components/Filter/Filter.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { useAuth } from '../../../context/useAuth.js';

import './MainPage.css';

const ACCESS_FILTERS = {
  PUBLIC: 'PUBLIC',
  PASSWORD: 'PASSWORD_PROTECTED',
  AUTHENTICATED: 'AUTHENTICATED',
};

const ACCESS_OPTIONS = [
  { value: ACCESS_FILTERS.PUBLIC, label: 'Públicas' },
  { value: ACCESS_FILTERS.PASSWORD, label: 'Con contraseña' },
  { value: ACCESS_FILTERS.AUTHENTICATED, label: 'Para usuarios registrados' },
];

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [3, 9, 18];
const COMPACT_PAGE_SIZE = 2;
const COMPACT_MEDIA_QUERY = '(max-width: 900px)';

function isCompactViewport() {
  return typeof window !== 'undefined' && Boolean(window.matchMedia?.(COMPACT_MEDIA_QUERY).matches);
}

function getLoadErrorMessage(error) {
  return error?.status
    ? error.message
    : 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
}

function PaginatedSurveySection({
  accessMode,
  categories,
  emptyMessage,
  icon: Icon,
  id,
  isAuthLoading,
  itemsPerPage,
  onPageSizeChange,
  pageSizeOptions,
  request,
  search,
  searchEmptyMessage,
  title,
  visible,
}) {
  const [surveys, setSurveys] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [requestVersion, setRequestVersion] = useState(0);
  useEffect(() => {
    if (!visible || isAuthLoading) return undefined;

    let isActive = true;

    Promise.resolve().then(() => {
      if (!isActive) return;
      setIsLoading(true);
      setErrorMessage('');
    });

    request({
      search,
      categories,
      accessMode,
      page: currentPage,
      size: itemsPerPage,
    })
      .then((response) => {
        if (!isActive) return;
        setSurveys(Array.isArray(response?.content) ? response.content : []);
        setTotalItems(Number(response?.totalElements ?? 0));
        setTotalPages(Number(response?.totalPages ?? 0));
      })
      .catch((error) => {
        if (!isActive) return;
        setSurveys([]);
        setTotalItems(0);
        setTotalPages(0);
        setErrorMessage(getLoadErrorMessage(error));
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => {
      isActive = false;
    };
  }, [
    accessMode,
    categories,
    currentPage,
    isAuthLoading,
    itemsPerPage,
    request,
    requestVersion,
    search,
    visible,
  ]);

  if (!visible) return null;

  return (
    <section
      className="main-page-survey-section"
      aria-busy={isLoading}
      aria-labelledby={id}
    >
      <header>
        <Icon aria-hidden="true" />
        <h3 id={id}>{title}</h3>
      </header>

      {errorMessage && (
        <div className="main-page-error" role="alert">
          <p>{errorMessage}</p>
          <button
            type="button"
            onClick={() => {
              setIsLoading(true);
              setErrorMessage('');
              setRequestVersion((version) => version + 1);
            }}
          >
            Reintentar
          </button>
        </div>
      )}

      {isLoading ? (
        <Skeleton
          variant="grid"
          count={Math.min(itemsPerPage, DEFAULT_PAGE_SIZE)}
          hasActions={false}
          label={`Cargando ${title.toLocaleLowerCase('es')}…`}
        />
      ) : !errorMessage && surveys.length > 0 ? (
        <div className="main-page-survey-grid">
          {surveys.map((survey) => (
            <SurveyCard
              key={survey.publicCode}
              survey={survey}
              headingLevel={4}
              variant="public"
              accessHref={`/${encodeURIComponent(survey.publicCode)}`}
            />
          ))}
        </div>
      ) : !errorMessage && (
        <div className="main-page-section-empty" role="status">
          <FaRegClipboard aria-hidden="true" />
          <p>{search || categories.length > 0 ? searchEmptyMessage : emptyMessage}</p>
        </div>
      )}

      {!isLoading && !errorMessage && totalItems > 0 && (
        <Pagination
          ariaLabel={`Paginación de ${title.toLowerCase()}`}
          currentPage={currentPage}
          itemLabel="encuestas"
          onPageChange={setCurrentPage}
          onPageSizeChange={(size) => {
            onPageSizeChange(size);
            setCurrentPage(0);
          }}
          pageSize={itemsPerPage}
          pageSizeLabel={`Encuestas por página en ${title.toLowerCase()}`}
          pageSizeOptions={pageSizeOptions}
          totalItems={totalItems}
          totalPages={totalPages}
        />
      )}
    </section>
  );
}

/**
 * Página principal de FormQuid.
 *
 * Presenta las encuestas disponibles agrupadas por su forma de acceso.
 */
export function MainPage() {
  const { isAuthenticated, isAuthLoading } = useAuth();
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedAccessModes, setSelectedAccessModes] = useState(
    () => ACCESS_OPTIONS.map((option) => option.value),
  );
  const [isCompact, setIsCompact] = useState(isCompactViewport);
  const [pageSizes, setPageSizes] = useState(() => Object.fromEntries(
    ACCESS_OPTIONS.map(({ value }) => [value, DEFAULT_PAGE_SIZE]),
  ));

  function updatePageSize(accessMode, size) {
    setPageSizes((current) => ({ ...current, [accessMode]: size }));
  }

  useEffect(() => {
    const mediaQuery = window.matchMedia?.(COMPACT_MEDIA_QUERY);
    if (!mediaQuery) return undefined;
    const handleChange = (event) => setIsCompact(event.matches);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  useEffect(() => {
    let active = true;
    getCategories({ surveyOnly: true })
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? response : []);
      })
      .catch(() => {});
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(search.trim());
    }, 300);

    return () => window.clearTimeout(timeoutId);
  }, [search]);
  const showPublicSection = selectedAccessModes.includes(ACCESS_FILTERS.PUBLIC);
  const showPasswordSection = selectedAccessModes.includes(ACCESS_FILTERS.PASSWORD);
  const showAuthenticatedSection = selectedAccessModes.includes(ACCESS_FILTERS.AUTHENTICATED);
  const availableAccessOptions = isAuthenticated
    ? ACCESS_OPTIONS
    : ACCESS_OPTIONS.filter((option) => option.value !== ACCESS_FILTERS.AUTHENTICATED);
  const visibleSelectedAccessModes = selectedAccessModes.filter((mode) => (
    isAuthenticated || mode !== ACCESS_FILTERS.AUTHENTICATED
  ));
  const request = isAuthenticated ? getRegisteredSurveys : getPublicSurveys;
  const pageSizeOptions = isCompact ? [] : PAGE_SIZE_OPTIONS;
  const categoryOptions = categories.map(({ code, name }) => ({ value: code, label: name }));

  return (
    <main className="main-page" aria-labelledby="main-page-title">
      <section className="main-page-hero" aria-labelledby="main-page-title">
        <picture className="main-page-hero-picture" aria-hidden="true">
          <img src={bannerSmall} alt="" />
        </picture>
        <div className="main-page-hero-content">
          <h1 id="main-page-title">La tecnología<br />desde tu perspectiva</h1>
          <p>
            Descubre cómo la infancia vive la tecnología y participa en
            encuestas que nos ayudan a comprender su mundo digital.
          </p>
        </div>
      </section>

      <div className="main-page-surveys">
        <div className="main-page-surveys-header">
          <div className="main-page-surveys-intro">
            <h2 id="available-surveys-title">Nuestras encuestas</h2>
            <p>Explora las encuestas disponibles y comparte tu opinión.</p>
          </div>

          <div className="main-page-controls" role="search">
            <label className="main-page-search">
              <span className="sr-only">Buscar encuestas por nombre, descripción o etiqueta</span>
              <MdSearch aria-hidden="true" />
              <input
                type="search"
                value={search}
                placeholder="Buscar"
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>
            <Filter
              label="Filtrar"
              options={availableAccessOptions}
              optionsLabel="Tipo de acceso"
              selectedValues={visibleSelectedAccessModes}
              choiceFilter={{
                label: 'Categoría',
                control: 'multiselect',
                allLabel: 'Todas las categorías',
                selectedCountLabel: 'categorías seleccionadas',
                options: categoryOptions,
                value: selectedCategories,
                resetValue: [],
              }}
              onChange={(accessModes, _text, categoriesSelection) => {
                setSelectedAccessModes(accessModes);
                setSelectedCategories(categoriesSelection);
              }}
              onReset={() => {
                setSearch('');
                setDebouncedSearch('');
              }}
            />
          </div>
        </div>

        <section className="main-page-results" aria-labelledby="available-surveys-title">
          {visibleSelectedAccessModes.length === 0 && (
            <div className="main-page-section-empty" role="status">
              <FaRegClipboard aria-hidden="true" />
              <p>Selecciona al menos un tipo de encuesta para ver los resultados.</p>
            </div>
          )}
          <PaginatedSurveySection
            key={JSON.stringify([ACCESS_FILTERS.PUBLIC, debouncedSearch, selectedCategories, isCompact])}
            accessMode={ACCESS_FILTERS.PUBLIC}
            categories={selectedCategories}
            emptyMessage="No hay encuestas públicas disponibles."
            icon={MdPublic}
            id="public-surveys-title"
            isAuthLoading={isAuthLoading}
            itemsPerPage={isCompact ? COMPACT_PAGE_SIZE : pageSizes[ACCESS_FILTERS.PUBLIC]}
            onPageSizeChange={(size) => updatePageSize(ACCESS_FILTERS.PUBLIC, size)}
            pageSizeOptions={pageSizeOptions}
            request={request}
            search={debouncedSearch}
            searchEmptyMessage="No hay encuestas públicas que coincidan con tu búsqueda."
            title="Encuestas públicas"
            visible={showPublicSection}
          />
          <PaginatedSurveySection
            key={JSON.stringify([ACCESS_FILTERS.PASSWORD, debouncedSearch, selectedCategories, isCompact])}
            accessMode={ACCESS_FILTERS.PASSWORD}
            categories={selectedCategories}
            emptyMessage="No hay encuestas con contraseña disponibles."
            icon={MdLockOutline}
            id="password-surveys-title"
            isAuthLoading={isAuthLoading}
            itemsPerPage={isCompact ? COMPACT_PAGE_SIZE : pageSizes[ACCESS_FILTERS.PASSWORD]}
            onPageSizeChange={(size) => updatePageSize(ACCESS_FILTERS.PASSWORD, size)}
            pageSizeOptions={pageSizeOptions}
            request={request}
            search={debouncedSearch}
            searchEmptyMessage="No hay encuestas con contraseña que coincidan con tu búsqueda."
            title="Encuestas con contraseña"
            visible={showPasswordSection}
          />
          {isAuthenticated && (
            <PaginatedSurveySection
              key={JSON.stringify([ACCESS_FILTERS.AUTHENTICATED, debouncedSearch, selectedCategories, isCompact])}
              accessMode={ACCESS_FILTERS.AUTHENTICATED}
              categories={selectedCategories}
              emptyMessage="No hay encuestas para usuarios registrados disponibles."
              icon={MdOutlinePerson}
              id="authenticated-surveys-title"
              isAuthLoading={isAuthLoading}
              itemsPerPage={isCompact ? COMPACT_PAGE_SIZE : pageSizes[ACCESS_FILTERS.AUTHENTICATED]}
              onPageSizeChange={(size) => updatePageSize(ACCESS_FILTERS.AUTHENTICATED, size)}
              pageSizeOptions={pageSizeOptions}
              request={request}
              search={debouncedSearch}
              searchEmptyMessage="No hay encuestas para usuarios registrados que coincidan con tu búsqueda."
              title="Encuestas para usuarios registrados"
              visible={showAuthenticatedSection}
            />
          )}
        </section>
      </div>
    </main>
  );
}
