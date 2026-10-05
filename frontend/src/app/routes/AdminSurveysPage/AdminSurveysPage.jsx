import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaRegClipboard } from 'react-icons/fa';
import { MdSearch } from 'react-icons/md';

import { getAdminSurveys } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';

import './AdminSurveysPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 30];

export function AdminSurveysPage() {
  const navigate = useNavigate();
  const [surveys, setSurveys] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [lifecycle, setLifecycle] = useState('ALL');
  const [visibility, setVisibility] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setCurrentPage(0);
      setDebouncedSearch(search.trim());
    }, 300);

    return () => window.clearTimeout(timeoutId);
  }, [search]);

  useEffect(() => {
    let isActive = true;

    setIsLoading(true);
    setErrorMessage('');

    getAdminSurveys({
      search: debouncedSearch,
      lifecycleStatus: lifecycle,
      visibility,
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
        setErrorMessage(error.message || 'No se han podido cargar las encuestas.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });

    return () => { isActive = false; };
  }, [currentPage, debouncedSearch, itemsPerPage, lifecycle, visibility]);

  return (
    <main className="admin-surveys-page">
      <div className="admin-surveys-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'ENCUESTAS' }]} />
        <header className="admin-surveys-header">
          <div>
            <h1>SUPERVISIÓN DE ENCUESTAS</h1>
            <p>Consulta todas las encuestas y accede a sus controles de estado y visibilidad.</p>
          </div>
        </header>

        <section className="admin-surveys-filters" aria-label="Filtros de encuestas">
          <div className="admin-surveys-search">
            <MdSearch aria-hidden="true" />
            <input type="search" value={search} placeholder="Buscar por título o propietario" aria-label="Buscar encuestas" onChange={(event) => setSearch(event.target.value)} />
          </div>
          <Dropdown value={lifecycle} aria-label="Filtrar por ciclo de vida" onChange={(event) => { setLifecycle(event.target.value); setCurrentPage(0); }}>
            <option value="ALL">Todos los estados</option>
            <option value="DRAFT">BORRADOR</option>
            <option value="OPEN">ABIERTAS</option>
            <option value="CLOSED">CERRADAS</option>
          </Dropdown>
          <Dropdown value={visibility} aria-label="Filtrar por visibilidad" onChange={(event) => { setVisibility(event.target.value); setCurrentPage(0); }}>
            <option value="ALL">Todas las visibilidades</option>
            <option value="LISTED">LISTADAS</option>
            <option value="UNLISTED">OCULTAS</option>
          </Dropdown>
        </section>

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}
        <section className="admin-surveys-results" aria-busy={isLoading}>
          {isLoading ? (
            <Skeleton variant="grid" count={Math.min(itemsPerPage, DEFAULT_PAGE_SIZE)} />
          ) : surveys.length === 0 ? (
            <div className="admin-surveys-empty" role="status">
              <FaRegClipboard aria-hidden="true" />
              <p>No hay encuestas que coincidan con los filtros.</p>
            </div>
          ) : (
            <div className="admin-surveys-grid">
              {surveys.map((survey) => (
                <SurveyCard
                  key={survey.id}
                  survey={survey}
                  headingLevel={2}
                  variant="admin"
                  onView={(id) => navigate(`/encuestas/${id}/editar?vista=detalles`)}
                  onSupervise={(id) => navigate(`/encuestas/${id}/editar?seccion=configuracion`)}
                />
              ))}
            </div>
          )}
        </section>

        {!isLoading && !errorMessage && totalItems > 0 && (
          <Pagination
            ariaLabel="Paginación de encuestas"
            currentPage={currentPage}
            itemLabel="encuestas"
            onPageChange={setCurrentPage}
            onPageSizeChange={(size) => {
              setItemsPerPage(size);
              setCurrentPage(0);
            }}
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
