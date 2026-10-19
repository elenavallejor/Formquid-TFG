import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaRegClipboard } from 'react-icons/fa';

import { getAdminSurveys } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
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

  function startLoading() {
    setIsLoading(true);
    setErrorMessage('');
  }

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setCurrentPage(0);
      setDebouncedSearch(search.trim());
    }, 300);

    return () => window.clearTimeout(timeoutId);
  }, [search]);

  useEffect(() => {
    let isActive = true;

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

        <SearchToolbar
          ariaLabel="Búsqueda y filtros de supervisión de encuestas"
          searchLabel="Buscar encuestas supervisadas"
          searchPlaceholder="Buscar"
          searchValue={search}
          onSearchChange={(event) => {
            if (event.target.value.trim() !== debouncedSearch || currentPage !== 0) startLoading();
            setSearch(event.target.value);
          }}
          filterChoices={[
            {
              label: 'Estado de la encuesta',
              options: [
                { value: 'ALL', label: 'Todos los estados' },
                { value: 'DRAFT', label: 'Borradores' },
                { value: 'OPEN', label: 'Abiertas' },
                { value: 'CLOSED', label: 'Cerradas' },
              ],
              value: lifecycle,
              resetValue: 'ALL',
            },
            {
              label: 'Visibilidad',
              options: [
                { value: 'ALL', label: 'Todas las visibilidades' },
                { value: 'LISTED', label: 'Listadas' },
                { value: 'UNLISTED', label: 'Ocultas' },
              ],
              value: visibility,
              resetValue: 'ALL',
            },
          ]}
          onFilterChange={(_values, _text, selectedChoices) => {
            if (selectedChoices[0] !== lifecycle || selectedChoices[1] !== visibility || currentPage !== 0) startLoading();
            setLifecycle(selectedChoices[0]);
            setVisibility(selectedChoices[1]);
            setCurrentPage(0);
          }}
          onFilterReset={() => {
            if (search || debouncedSearch || currentPage !== 0) startLoading();
            setSearch('');
            setDebouncedSearch('');
            setCurrentPage(0);
          }}
        />

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
            onPageChange={(page) => {
              if (page !== currentPage) startLoading();
              setCurrentPage(page);
            }}
            onPageSizeChange={(size) => {
              if (size !== itemsPerPage || currentPage !== 0) startLoading();
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
