import { useEffect, useRef, useState } from 'react';
import { MdClose, MdOutlineAssignment, MdOutlineRemoveRedEye } from 'react-icons/md';
import { useParams } from 'react-router-dom';

import { getSurveyParticipation, getSurveyParticipations } from '../../../api/participationApi.js';
import { getSurveyForResults } from '../../../api/surveysApi.js';
import { BackButton } from '../../../components/BackButton/BackButton.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { formatDateTime } from '../../../utils/formatDate.js';

import '../ElementDetailPage/ElementDetailPage.css';
import './SurveyParticipationsPage.css';

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];

const QUESTION_TYPE_LABELS = {
  TEXT: 'Texto',
  NUMBER: 'Número',
  SCALE: 'Escala',
  BOOLEAN: 'Sí / No',
  SINGLE_CHOICE: 'Selección única',
  MULTIPLE_CHOICE: 'Selección múltiple',
  MATRIX_SCALE: 'Matriz de escala',
};

function ScalarAnswer({ answer }) {
  if (answer.textValue != null) return <p className="participation-text-answer">{answer.textValue}</p>;
  if (answer.numericValue != null) return <p className="participation-scalar-answer">{answer.numericValue}</p>;
  if (answer.booleanValue != null) {
    return (
      <p className="participation-scalar-answer">
        {answer.booleanValue ? (answer.trueLabel || 'Sí') : (answer.falseLabel || 'No')}
      </p>
    );
  }
  return null;
}

function AnswerDetail({ answer, index }) {
  const options = Array.isArray(answer.selectedOptions) ? answer.selectedOptions : [];
  const matrixValues = Array.isArray(answer.matrixValues) ? answer.matrixValues : [];

  return (
    <article className="participation-answer-card">
      <header>
        <span>Respuesta {index + 1}</span>
        <strong>{QUESTION_TYPE_LABELS[answer.questionType] || answer.questionType}</strong>
      </header>
      <h3>{answer.statement}</h3>
      <ScalarAnswer answer={answer} />
      {options.length > 0 && (
        <ul className="participation-option-list">
          {options.map((option) => (
            <li key={option.optionId}>
              <strong>{option.label}</strong>
              {option.otherText && <span>{option.otherText}</span>}
            </li>
          ))}
        </ul>
      )}
      {matrixValues.length > 0 && (
        <dl className="participation-matrix-values">
          {matrixValues.map((value) => (
            <div key={value.rowId}>
              <dt>{value.label}</dt>
              <dd>{value.numericValue}</dd>
            </div>
          ))}
        </dl>
      )}
    </article>
  );
}

export function SurveyParticipationsPage() {
  const { surveyId } = useParams();
  const [survey, setSurvey] = useState(null);
  const [participations, setParticipations] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [surveyError, setSurveyError] = useState('');
  const [listError, setListError] = useState('');
  const [detailError, setDetailError] = useState('');
  const detailRequest = useRef(0);

  useEffect(() => {
    let isActive = true;
    getSurveyForResults(surveyId)
      .then((surveyResponse) => {
        if (!isActive) return;
        setSurvey(surveyResponse);
        setSurveyError('');
      })
      .catch((error) => {
        if (!isActive) return;
        setSurvey(null);
        setSurveyError(error.message || 'No se ha podido cargar la encuesta.');
      });
    return () => { isActive = false; };
  }, [surveyId]);

  useEffect(() => {
    let isActive = true;
    const abortController = new AbortController();
    Promise.resolve()
      .then(() => {
        if (isActive) {
          setIsLoading(true);
          setListError('');
        }
        return getSurveyParticipations(surveyId, {
          page: currentPage,
          size: pageSize,
          signal: abortController.signal,
        });
      })
      .then((participationResponse) => {
        if (!isActive) return;
        setParticipations(Array.isArray(participationResponse?.content) ? participationResponse.content : []);
        setTotalElements(Number(participationResponse?.totalElements ?? 0));
        setTotalPages(Number(participationResponse?.totalPages ?? 0));
      })
      .catch((error) => {
        if (!isActive || error.name === 'AbortError') return;
        setParticipations([]);
        setTotalElements(0);
        setTotalPages(0);
        setListError(error.message || 'No se han podido cargar las participaciones.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });
    return () => {
      isActive = false;
      abortController.abort();
    };
  }, [currentPage, pageSize, surveyId]);

  function changePage(page) {
    setCurrentPage(page);
    setSelectedId(null);
    setDetail(null);
    setDetailError('');
    setIsDetailLoading(false);
    detailRequest.current += 1;
  }

  function changePageSize(size) {
    setPageSize(size);
    changePage(0);
  }

  async function showParticipation(participationId) {
    const requestId = detailRequest.current + 1;
    detailRequest.current = requestId;
    setSelectedId(participationId);
    setDetail(null);
    setDetailError('');
    setIsDetailLoading(true);
    try {
      const response = await getSurveyParticipation(surveyId, participationId);
      if (detailRequest.current !== requestId) return;
      setDetail(response);
    } catch (error) {
      if (detailRequest.current !== requestId) return;
      setDetailError(error.message || 'No se ha podido cargar esta participación.');
    } finally {
      if (detailRequest.current === requestId) setIsDetailLoading(false);
    }
  }

  function closeDetail() {
    detailRequest.current += 1;
    setSelectedId(null);
    setDetail(null);
    setDetailError('');
    setIsDetailLoading(false);
  }

  return (
    <main className="catalog-detail-page survey-participations-page">
      <div className="catalog-detail-container survey-participations-container">
        <BackButton fallbackTo={`/encuestas/${surveyId}/resultados`} className="catalog-detail-back" />
        <Breadcrumbs
          className="catalog-detail-breadcrumb"
          items={[
            { label: 'PANEL', to: '/panel' },
            { label: 'RESULTADOS', to: '/resultados' },
            { label: survey?.title || 'ENCUESTA', to: `/encuestas/${surveyId}/resultados` },
            { label: 'PARTICIPACIONES' },
          ]}
        />

        <header className="catalog-detail-page-header">
          <h1>PARTICIPACIONES INDIVIDUALES</h1>
          <p>Consulta las respuestas enviadas sin mostrar la identidad de las personas participantes.</p>
        </header>

        {surveyError && <ErrorMessage message={surveyError} onClose={() => setSurveyError('')} />}
        {listError && <ErrorMessage message={listError} onClose={() => setListError('')} />}

        <article className="catalog-detail-card survey-participations-card">
          <header className="catalog-detail-title-bar survey-participations-title">
            <div>
              <MdOutlineAssignment aria-hidden="true" />
              <h2>{survey?.title || 'Participaciones'}</h2>
            </div>
            <span>{totalElements} {totalElements === 1 ? 'participación' : 'participaciones'}</span>
          </header>

          <div className="survey-participations-body" aria-busy={isLoading}>
            {isLoading ? (
              <Skeleton variant="list" count={5} />
            ) : listError ? null : participations.length === 0 ? (
              <div className="survey-participations-empty" role="status">
                Esta encuesta todavía no tiene participaciones.
              </div>
            ) : (
              <ul className="survey-participations-list">
                {participations.map((participation) => (
                  <li key={participation.id} className={selectedId === participation.id ? 'selected' : ''}>
                    <div>
                      <strong>Participación #{participation.id}</strong>
                      <time dateTime={participation.submittedAt}>{formatDateTime(participation.submittedAt)}</time>
                    </div>
                    <button
                      type="button"
                      aria-controls="participation-detail"
                      aria-expanded={selectedId === participation.id}
                      disabled={isDetailLoading && selectedId === participation.id}
                      onClick={() => showParticipation(participation.id)}
                    >
                      <MdOutlineRemoveRedEye aria-hidden="true" />
                      Ver respuestas
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {!isLoading && totalElements > 0 && (
              <Pagination
                ariaLabel="Paginación de participaciones"
                currentPage={currentPage}
                itemLabel="participaciones"
                onPageChange={changePage}
                onPageSizeChange={changePageSize}
                pageSize={pageSize}
                pageSizeLabel="Participaciones por página"
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                totalItems={totalElements}
                totalPages={totalPages}
              />
            )}
          </div>
        </article>

        {selectedId && (
          <section id="participation-detail" className="participation-detail" aria-busy={isDetailLoading} aria-live="polite">
            <header>
              <div>
                <span>DETALLE DE SOLO LECTURA</span>
                <h2>Participación #{selectedId}</h2>
                {detail?.submittedAt && <time dateTime={detail.submittedAt}>{formatDateTime(detail.submittedAt)}</time>}
              </div>
              <button type="button" className="participation-detail-close" aria-label="Cerrar detalle" onClick={closeDetail}>
                <MdClose aria-hidden="true" />
              </button>
            </header>

            {detailError && <ErrorMessage message={detailError} onClose={() => setDetailError('')} />}
            {isDetailLoading ? (
              <Skeleton variant="list" count={4} />
            ) : detail && (
              detail.answers?.length > 0
                ? <div className="participation-answers">{detail.answers.map((answer, index) => <AnswerDetail key={answer.answerId} answer={answer} index={index} />)}</div>
                : <p className="survey-participations-empty">Esta participación no contiene respuestas.</p>
            )}
          </section>
        )}
      </div>
    </main>
  );
}
