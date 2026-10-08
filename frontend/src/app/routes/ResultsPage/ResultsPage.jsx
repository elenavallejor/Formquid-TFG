import { useEffect, useState } from 'react';
import { FaRegClipboard } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

import { getResultSurveys } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';

import './ResultsPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 30];
const SEARCH_OPTIONS = [
  { value: 'TITLE', label: 'Título' },
  { value: 'OWNER', label: 'Editor o propietario' },
];
const LIFECYCLE_OPTIONS = [
  { value: 'ALL', label: 'Todos los estados' },
  { value: 'DRAFT', label: 'Borradores' },
  { value: 'OPEN', label: 'Abiertas' },
  { value: 'CLOSED', label: 'Cerradas' },
];
const SORT_OPTIONS = [
  { value: 'NEWEST', label: 'Más reciente' },
  { value: 'OLDEST', label: 'Más antigua' },
];

export function ResultsPage() {
  const navigate = useNavigate();
  const [surveys, setSurveys] = useState([]);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedSearchFields, setSelectedSearchFields] = useState(
    () => SEARCH_OPTIONS.map((option) => option.value),
  );
  const [lifecycle, setLifecycle] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setCurrentPage(0);
      setDebouncedSearch(search.trim());
    }, 300);
    return () => window.clearTimeout(timeoutId);
  }, [search]);

  useEffect(() => {
    let isActive = true;
    const abortController = new AbortController();
    const request = getResultSurveys({
      search: debouncedSearch,
      searchFields: selectedSearchFields,
      lifecycleStatus: lifecycle,
      sortOrder,
      page: currentPage,
      size: itemsPerPage,
      signal: abortController.signal,
    });

    Promise.resolve()
      .then(() => {
        if (isActive) {
          setIsLoading(true);
          setErrorMessage('');
        }
        return request;
      })
      .then((response) => {
        if (!isActive) return;
        setSurveys(Array.isArray(response?.content) ? response.content : []);
        setTotalItems(Number(response?.totalElements ?? 0));
        setTotalPages(Number(response?.totalPages ?? 0));
      })
      .catch((error) => {
        if (!isActive || error.name === 'AbortError') return;
        setSurveys([]);
        setTotalItems(0);
        setTotalPages(0);
        setErrorMessage(error.message || 'No se han podido cargar las encuestas.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => {
      isActive = false;
      abortController.abort();
    };
  }, [currentPage, debouncedSearch, itemsPerPage, lifecycle, selectedSearchFields, sortOrder]);

  return (
    <main className="results-page">
      <div className="results-page-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'RESULTADOS' }]} />

        <header className="results-page-header">
          <div>
            <h1>RESULTADOS Y ESTADÍSTICAS</h1>
            <p>Selecciona una encuesta para consultar sus respuestas agregadas y su análisis ponderado.</p>
          </div>
        </header>

        <SearchToolbar
          ariaLabel="Búsqueda y filtros de encuestas con resultados"
          searchLabel="Buscar encuestas con resultados"
          searchPlaceholder="Buscar"
          searchValue={search}
          onSearchChange={(event) => setSearch(event.target.value)}
          filterOptions={SEARCH_OPTIONS}
          filterOptionsLabel="Filtrar por"
          selectedFilterValues={selectedSearchFields}
          filterChoice={{
            label: 'Estado de la encuesta',
            options: LIFECYCLE_OPTIONS,
            value: lifecycle,
            resetValue: 'ALL',
          }}
          onFilterChange={(values, _text, selectedLifecycle) => {
            setSelectedSearchFields(values);
            setLifecycle(selectedLifecycle);
            setCurrentPage(0);
          }}
          onFilterReset={() => setCurrentPage(0)}
          sortAriaLabel="Ordenar encuestas con resultados"
          sortOptions={SORT_OPTIONS}
          sortValue={sortOrder}
          onSortChange={(event) => {
            setSortOrder(event.target.value);
            setCurrentPage(0);
          }}
        />

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}

        <section className="results-page-content" aria-busy={isLoading} aria-live="polite">
          {isLoading ? (
            <Skeleton variant="grid" count={6} />
          ) : surveys.length === 0 ? (
            <div className="results-page-empty" role="status">
              <FaRegClipboard aria-hidden="true" />
              <h2>No hay encuestas con respuestas</h2>
              <p>No se han encontrado encuestas con respuestas registradas que coincidan con los filtros.</p>
            </div>
          ) : (
            <div className="results-survey-grid">
              {surveys.map((survey) => (
                <SurveyCard
                  key={survey.id}
                  survey={survey}
                  headingLevel={2}
                  variant="results"
                  onView={(id) => navigate(`/encuestas/${id}/resultados`)}
                />
              ))}
            </div>
          )}
        </section>

        {!isLoading && !errorMessage && totalItems > 0 && (
          <Pagination
            ariaLabel="Paginación de encuestas con resultados"
            currentPage={currentPage}
            itemLabel="encuestas"
            onPageChange={setCurrentPage}
            onPageSizeChange={(size) => { setItemsPerPage(size); setCurrentPage(0); }}
            pageSize={itemsPerPage}
            pageSizeLabel="Encuestas por página"
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            totalItems={totalItems}
            totalPages={totalPages}
          />
        )}
      </div>
    </main>
  );
}
