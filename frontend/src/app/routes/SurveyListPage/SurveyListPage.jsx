import { useEffect, useState } from 'react';
import { FaRegClipboard } from 'react-icons/fa';
import { MdSearch } from 'react-icons/md';

import { createCollaborationRequest } from '../../../api/collaborationsApi.js';
import { getSurveyDirectory } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './SurveyListPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 30];
const SEARCH_DEBOUNCE_MS = 300;

export function SurveyListPage() {
  const { user } = useAuth();
  const [surveys, setSurveys] = useState([]);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [lifecycle, setLifecycle] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('RECENT');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [requestingSurveyId, setRequestingSurveyId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setCurrentPage(0);
      setDebouncedSearch(search.trim());
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeoutId);
  }, [search]);

  useEffect(() => {
    let isActive = true;

    setIsLoading(true);
    setErrorMessage('');

    getSurveyDirectory({
      search: debouncedSearch,
      lifecycleStatus: lifecycle,
      sortOrder,
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
  }, [currentPage, debouncedSearch, itemsPerPage, lifecycle, sortOrder]);

  async function requestCollaboration(surveyId) {
    if (!user?.id || requestingSurveyId !== null) return;

    setRequestingSurveyId(surveyId);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await createCollaborationRequest(surveyId, {
        userId: user.id,
        permission: 'EDIT',
        requestType: 'ACCESS_REQUEST',
        message: null,
      });
      setSurveys((current) => current.map((survey) => (
        survey.id === surveyId
          ? { ...survey, collaborationRequestPending: true }
          : survey
      )));
      setSuccessMessage('Solicitud de colaboración enviada al editor responsable.');
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido solicitar la colaboración.');
    } finally {
      setRequestingSurveyId(null);
    }
  }

  return (
    <main className="survey-list-page">
      <div className="survey-list-container">
        <Breadcrumbs items={[{ label: 'PANEL', to: '/panel' }, { label: 'ENCUESTAS' }]} />

        <header className="survey-list-header">
          <h1>TODAS LAS ENCUESTAS</h1>
          <p>Explora las encuestas de otros editores y solicita colaborar en su creación.</p>
        </header>

        <section className="survey-list-filters" aria-label="Filtros de encuestas">
          <div className="survey-list-search">
            <MdSearch aria-hidden="true" />
            <input
              type="search"
              value={search}
              placeholder="Buscar por título o editor"
              aria-label="Buscar encuestas"
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>

          <Dropdown value={lifecycle} aria-label="Filtrar por estado" onChange={(event) => { setLifecycle(event.target.value); setCurrentPage(0); }}>
            <option value="ALL">Todos los estados</option>
            <option value="DRAFT">BORRADORES</option>
            <option value="OPEN">ABIERTAS</option>
            <option value="CLOSED">CERRADAS</option>
          </Dropdown>

          <Dropdown value={sortOrder} aria-label="Ordenar encuestas" onChange={(event) => { setSortOrder(event.target.value); setCurrentPage(0); }}>
            <option value="RECENT">Más recientes primero</option>
            <option value="OLDEST">Más antiguas primero</option>
          </Dropdown>
        </section>

        {errorMessage && <ErrorMessage message={errorMessage} onClose={() => setErrorMessage('')} />}
        {successMessage && <p className="survey-list-success" role="status">{successMessage}</p>}

        <section className="survey-list-results" aria-busy={isLoading}>
          {isLoading ? (
            <Skeleton variant="grid" count={Math.min(itemsPerPage, DEFAULT_PAGE_SIZE)} />
          ) : surveys.length === 0 ? (
            <div className="survey-list-empty" role="status">
              <FaRegClipboard aria-hidden="true" />
              <p>No hay encuestas que coincidan con los filtros.</p>
            </div>
          ) : (
            <div className="survey-list-grid">
              {surveys.map((survey) => (
                <SurveyCard
                  key={survey.id}
                  survey={survey}
                  headingLevel={2}
                  variant="directory"
                  isRequesting={requestingSurveyId === survey.id}
                  onRequestCollaboration={requestCollaboration}
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
