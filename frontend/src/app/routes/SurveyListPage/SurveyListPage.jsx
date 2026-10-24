import { useEffect, useState } from 'react';
import { FaRegClipboard } from 'react-icons/fa';

import { createCollaborationRequest } from '../../../api/collaborationsApi.js';
import { getSurveyDirectory } from '../../../api/surveysApi.js';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { useAuth } from '../../../context/useAuth.js';

import './SurveyListPage.css';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [9, 18, 30];
const SEARCH_DEBOUNCE_MS = 300;

export function SurveyListPage() {
  const { user } = useAuth();
  const [surveys, setSurveys] = useState([]);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [lifecycles, setLifecycles] = useState([]);
  const [sortOrder, setSortOrder] = useState('RECENT');
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [requestingSurveyId, setRequestingSurveyId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  function startLoading() {
    setIsLoading(true);
    setErrorMessage('');
  }

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setCurrentPage(0);
      setDebouncedSearch(search.trim());
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeoutId);
  }, [search]);

  useEffect(() => {
    let isActive = true;

    getSurveyDirectory({
      search: debouncedSearch,
      lifecycleStatuses: lifecycles,
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
  }, [currentPage, debouncedSearch, itemsPerPage, lifecycles, sortOrder]);

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

        <SearchToolbar
          ariaLabel="Búsqueda y filtros del listado de encuestas"
          searchLabel="Buscar encuestas"
          searchPlaceholder="Buscar"
          searchValue={search}
          onSearchChange={(event) => {
            if (event.target.value.trim() !== debouncedSearch || currentPage !== 0) startLoading();
            setSearch(event.target.value);
          }}
          filterChoices={[{
            label: 'Estado de la encuesta',
            options: [
              { value: 'DRAFT', label: 'Borradores' },
              { value: 'OPEN', label: 'Abiertas' },
              { value: 'CLOSED', label: 'Cerradas' },
            ],
            control: 'multiselect',
            value: lifecycles,
            resetValue: [],
            allLabel: 'Todos los estados',
            selectedCountLabel: 'estados seleccionados',
          }]}
          onFilterChange={(_values, _text, selectedChoices) => {
            if (selectedChoices[0].join() !== lifecycles.join() || currentPage !== 0) startLoading();
            setLifecycles(selectedChoices[0]);
            setCurrentPage(0);
          }}
          onFilterReset={() => {
            if (search || debouncedSearch || currentPage !== 0) startLoading();
            setSearch('');
            setDebouncedSearch('');
            setCurrentPage(0);
          }}
          sortAriaLabel="Ordenar encuestas"
          sortOptions={[
            { value: 'RECENT', label: 'Más recientes primero' },
            { value: 'OLDEST', label: 'Más antiguas primero' },
          ]}
          sortValue={sortOrder}
          onSortChange={(event) => {
            if (event.target.value !== sortOrder || currentPage !== 0) startLoading();
            setSortOrder(event.target.value);
            setCurrentPage(0);
          }}
        />

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
