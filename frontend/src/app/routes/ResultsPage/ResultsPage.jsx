import { useEffect, useState } from 'react';
import { FaRegClipboard } from 'react-icons/fa';
import { MdSearch } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';

import { getAdminSurveys, getMySurveys } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './ResultsPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 30];

export function ResultsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [surveys, setSurveys] = useState([]);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [lifecycle, setLifecycle] = useState('ALL');
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
    if (!user?.role) return undefined;
    let isActive = true;
    const isAdmin = user.role === 'ADMIN' || user.role === 'SUPERADMIN';
    const request = isAdmin
      ? getAdminSurveys({
        search: debouncedSearch,
        lifecycleStatus: lifecycle,
        page: currentPage,
        size: itemsPerPage,
      })
      : getMySurveys({
        search: debouncedSearch,
        lifecycleStatus: lifecycle,
        page: currentPage,
        size: itemsPerPage,
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
        if (!isActive) return;
        setSurveys([]);
        setTotalItems(0);
        setTotalPages(0);
        setErrorMessage(error.message || 'No se han podido cargar las encuestas.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => { isActive = false; };
  }, [currentPage, debouncedSearch, itemsPerPage, lifecycle, user?.role]);

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

        <section className="results-page-filters" aria-label="Filtros de encuestas">
          <div className="results-page-search">
            <MdSearch aria-hidden="true" />
            <input
              type="search"
              value={search}
              placeholder="Buscar por título"
              aria-label="Buscar encuestas por título"
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <Dropdown
            value={lifecycle}
            aria-label="Filtrar por estado"
            onChange={(event) => { setLifecycle(event.target.value); setCurrentPage(0); }}
          >
            <option value="ALL">Todos los estados</option>
            <option value="DRAFT">BORRADORES</option>
            <option value="OPEN">ABIERTAS</option>
            <option value="CLOSED">CERRADAS</option>
          </Dropdown>
        </section>

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}

        <section className="results-page-content" aria-busy={isLoading} aria-live="polite">
          {isLoading ? (
            <Skeleton variant="grid" count={6} />
          ) : surveys.length === 0 ? (
            <div className="results-page-empty" role="status">
              <FaRegClipboard aria-hidden="true" />
              <h2>No hay encuestas disponibles</h2>
              <p>No se han encontrado encuestas que coincidan con los filtros.</p>
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
