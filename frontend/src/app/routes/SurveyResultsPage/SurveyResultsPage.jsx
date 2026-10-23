import { useEffect, useMemo, useState } from 'react';
import {
  MdAssessment,
  MdInfoOutline,
  MdOutlineAnalytics,
  MdOutlineRemoveRedEye,
} from 'react-icons/md';
import { useNavigate, useParams } from 'react-router-dom';

import { getSurveyStatistics } from '../../../api/statisticsApi.js';
import { getSurveyForResults } from '../../../api/surveysApi.js';
import { BackButton } from '../../../components/BackButton/BackButton.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';
import { ErrorMessage } from '../../../components/ErrorMessage/ErrorMessage.jsx';
import { Question } from '../../../components/Question/Question.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { buildBlockQuestionFromCatalog } from '../../../utils/blockBuilderUtils.js';
import { formatDateTime } from '../../../utils/formatDate.js';
import { QUESTION_TYPES } from '../../../utils/questionBuilderUtils.js';

import '../ElementDetailPage/ElementDetailPage.css';
import '../../../components/Table/Table.css';
import './SurveyResultsPage.css';

const DIRECTION_LABELS = {
  POSITIVE: 'Positiva',
  NEGATIVE: 'Negativa',
  NEUTRAL: 'No evaluable',
};

function formatNumber(value, maximumFractionDigits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—';
  return new Intl.NumberFormat('es-ES', { maximumFractionDigits }).format(Number(value));
}

function formatPercentage(value) {
  return `${formatNumber(value, 1)} %`;
}

function collectQuestionSettings(contentItems = []) {
  const settings = new Map();
  const collect = (items) => {
    (items || []).forEach((item) => {
      if (item.itemType === 'QUESTION' && item.question?.id) {
        settings.set(item.question.id, {
          ...item.question,
          isRequired: Boolean(item.required ?? item.isRequired),
        });
      }
      if (item.itemType === 'BLOCK') {
        collect(item.block?.contentItems || item.block?.items || []);
      }
    });
  };
  collect(contentItems);
  return settings;
}

function PercentageBars({ items, ariaLabel }) {
  if (!items?.length) return null;
  return (
    <div className="statistics-bars" aria-label={ariaLabel}>
      {items.map((item) => {
        const percentage = Math.max(0, Math.min(100, Number(item.percentage) || 0));
        return (
          <div className="statistics-bar-row" key={item.key ?? item.label}>
            <div className="statistics-bar-label">
              <span>{item.label}</span>
              <strong>{formatPercentage(percentage)}</strong>
            </div>
            <div
              className="statistics-bar-track"
              role="img"
              aria-label={`${item.label}: ${formatPercentage(percentage)}, ${item.count} respuestas`}
            >
              <span className="statistics-bar-fill" style={{ width: `${percentage}%` }} />
            </div>
            <span className="statistics-bar-count">{item.count} {item.count === 1 ? 'respuesta' : 'respuestas'}</span>
          </div>
        );
      })}
    </div>
  );
}

function NumericSummary({ data }) {
  if (data?.average === null || data?.average === undefined) return null;
  const values = [
    ['Media', data.average],
    ['Mediana', data.median],
    ['Mínimo', data.min],
    ['Máximo', data.max],
    ['Desviación', data.standardDeviation],
  ];
  return (
    <dl className="numeric-summary">
      {values.map(([label, value]) => (
        <div key={label}><dt>{label}</dt><dd>{formatNumber(value, 2)}</dd></div>
      ))}
    </dl>
  );
}

function EvaluationBadge({ score, question }) {
  if (score === null || score === undefined || !question) return null;
  return (
    <div className="question-evaluation" aria-label="Configuración de evaluación">
      <span>Peso <strong>{question.evaluationWeight > 0 ? '+' : ''}{formatNumber(question.evaluationWeight, 2)}</strong></span>
      <span>Dirección <strong>{DIRECTION_LABELS[question.evaluationDirection] || question.evaluationDirection}</strong></span>
      <span>Índice <strong>{formatNumber(score, 1)}</strong></span>
    </div>
  );
}

function QuestionStatistics({ statistics, index, question }) {
  const [isQuestionVisible, setIsQuestionVisible] = useState(false);
  const typeConfig = QUESTION_TYPES.find((type) => type.id === statistics.questionType);
  const TypeIcon = typeConfig?.icon;
  const questionDetailId = `question-detail-${statistics.questionId}`;
  const questionPreview = question ? {
    ...buildBlockQuestionFromCatalog(question),
    isRequired: Boolean(question.isRequired),
  } : null;
  const choiceItems = (statistics.choiceStatistics || []).map((item) => ({
    key: item.optionId,
    label: item.label,
    count: item.count,
    percentage: item.percentage,
  }));
  const booleanItems = statistics.booleanStatistics ? [
    {
      key: 'true',
      label: question?.settings?.trueLabel || 'Sí',
      count: statistics.booleanStatistics.trueCount,
      percentage: statistics.booleanStatistics.truePercentage,
    },
    {
      key: 'false',
      label: question?.settings?.falseLabel || 'No',
      count: statistics.booleanStatistics.falseCount,
      percentage: statistics.booleanStatistics.falsePercentage,
    },
  ] : [];
  const valueItems = (statistics.valueDistribution || []).map((item) => ({
    key: String(item.value),
    label: String(item.value),
    count: item.count,
    percentage: item.percentage,
  }));

  return (
    <article className="question-statistics-card">
      <header className="question-statistics-header">
        <div>
          <span className="question-statistics-number">Pregunta {index + 1}</span>
          <h3>{statistics.statement}</h3>
          <span
            className="question-statistics-type"
            style={typeConfig ? {
              color: typeConfig.color,
              borderColor: `${typeConfig.color}60`,
              backgroundColor: `${typeConfig.color}14`,
            } : undefined}
          >
            {TypeIcon && <TypeIcon aria-hidden="true" />}
            {typeConfig?.title || statistics.questionType}
          </span>
        </div>
        <div className="question-statistics-side">
          <div className="question-response-rate">
            <strong>{formatPercentage(statistics.responseRate)}</strong>
            <span>Tasa de respuesta</span>
          </div>
          {questionPreview && (
            <button
              type="button"
              className="question-view-button"
              aria-controls={questionDetailId}
              aria-expanded={isQuestionVisible}
              onClick={() => setIsQuestionVisible((isVisible) => !isVisible)}
            >
              <MdOutlineRemoveRedEye aria-hidden="true" />
              <span>{isQuestionVisible ? 'Ocultar pregunta' : 'Ver pregunta'}</span>
            </button>
          )}
        </div>
      </header>

      {isQuestionVisible && questionPreview && (
        <section id={questionDetailId} className="question-detail-preview" aria-labelledby={`${questionDetailId}-title`}>
          <h4 id={`${questionDetailId}-title`}>Vista detallada de la pregunta</h4>
          <Question
            questionNumber={`${index + 1}`}
            headingLevel={5}
            data={questionPreview}
            interactive={false}
          />
        </section>
      )}

      <div className="question-statistics-counts">
        <span><strong>{statistics.answers}</strong> respuestas</span>
        <span><strong>{statistics.omissions}</strong> omisiones</span>
      </div>

      {choiceItems.length > 0 && (
        <>
          <h4>Distribución por opción</h4>
          <PercentageBars items={choiceItems} ariaLabel="Distribución porcentual por opción" />
          {statistics.questionType === 'MULTIPLE_CHOICE' && (
            <p className="statistics-help"><MdInfoOutline aria-hidden="true" /> Cada porcentaje representa participantes que marcaron la opción; la suma puede superar el 100 %.</p>
          )}
        </>
      )}

      {booleanItems.length > 0 && (
        <>
          <h4>Distribución por opción</h4>
          <PercentageBars items={booleanItems} ariaLabel="Distribución porcentual de respuestas" />
        </>
      )}

      {valueItems.length > 0 && (
        <>
          <h4>Distribución de la escala</h4>
          <PercentageBars items={valueItems} ariaLabel="Distribución porcentual de la escala" />
        </>
      )}

      <NumericSummary data={statistics.numericStatistics} />

      {(statistics.matrixStatistics || []).length > 0 && (
        <div className="matrix-statistics">
          <h4>Resultados por fila</h4>
          {statistics.matrixStatistics.map((row) => (
            <section className="matrix-statistics-row" key={row.rowId}>
              <div className="matrix-statistics-heading">
                <h5>{row.label}</h5>
                <span>{row.answers} {row.answers === 1 ? 'respuesta' : 'respuestas'}</span>
              </div>
              <PercentageBars
                ariaLabel={`Distribución de ${row.label}`}
                items={(row.valueDistribution || []).map((item) => ({
                  key: String(item.value),
                  label: String(item.value),
                  count: item.count,
                  percentage: item.percentage,
                }))}
              />
              <NumericSummary data={row.numericStatistics} />
            </section>
          ))}
        </div>
      )}

      {statistics.questionType === 'TEXT' && (
        <p className="statistics-text-note">Las respuestas de texto libre se consultarán desde el detalle de participaciones individuales.</p>
      )}

      <EvaluationBadge score={statistics.evaluationScore} question={question} />
    </article>
  );
}

function WeightedAnalysis({ score, hasParticipations }) {
  if (!hasParticipations) {
    return (
      <section className="weighted-analysis weighted-analysis--empty" aria-labelledby="weighted-analysis-title">
        <MdOutlineAnalytics aria-hidden="true" />
        <div><h2 id="weighted-analysis-title">Análisis final ponderado</h2><p>El análisis estará disponible cuando se registre alguna participación.</p></div>
      </section>
    );
  }
  if (score === null || score === undefined) {
    return (
      <section className="weighted-analysis weighted-analysis--empty" aria-labelledby="weighted-analysis-title">
        <MdOutlineAnalytics aria-hidden="true" />
        <div><h2 id="weighted-analysis-title">Análisis final ponderado</h2><p>No hay preguntas evaluables con peso configurado para calcular un resultado global.</p></div>
      </section>
    );
  }

  const normalizedPosition = Math.max(0, Math.min(100, (Number(score) + 100) / 2));
  const tendency = score > 20 ? 'Tendencia positiva' : score < -20 ? 'Tendencia negativa' : 'Tendencia equilibrada';
  return (
    <section className="weighted-analysis" aria-labelledby="weighted-analysis-title">
      <div className="weighted-analysis-copy">
        <span className="weighted-analysis-kicker">RESULTADO GLOBAL</span>
        <h2 id="weighted-analysis-title">Análisis final ponderado</h2>
        <p>Combina el índice de las preguntas evaluables según el peso asignado a cada una.</p>
      </div>
      <div className="weighted-analysis-result">
        <strong>{formatNumber(score, 1)}</strong>
        <span>{tendency}</span>
      </div>
      <div className="weighted-scale" role="img" aria-label={`Índice ponderado ${formatNumber(score, 1)} sobre una escala de menos 100 a 100`}>
        <span className="weighted-scale-marker" style={{ left: `${normalizedPosition}%` }} />
      </div>
      <div className="weighted-scale-labels"><span>−100</span><span>0</span><span>100</span></div>
    </section>
  );
}

export function SurveyResultsPage() {
  const { surveyId } = useParams();
  const navigate = useNavigate();
  const [survey, setSurvey] = useState(null);
  const [statistics, setStatistics] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let isActive = true;
    Promise.resolve()
      .then(() => {
        if (isActive) {
          setIsLoading(true);
          setErrorMessage('');
        }
        return Promise.all([getSurveyForResults(surveyId), getSurveyStatistics(surveyId)]);
      })
      .then(([surveyResponse, statisticsResponse]) => {
        if (!isActive) return;
        setSurvey(surveyResponse);
        setStatistics(statisticsResponse);
      })
      .catch((error) => {
        if (!isActive) return;
        setSurvey(null);
        setStatistics(null);
        setErrorMessage(error.message || 'No se han podido cargar los resultados de la encuesta.');
      })
      .finally(() => {
        if (isActive) setIsLoading(false);
      });
    return () => { isActive = false; };
  }, [surveyId]);

  const questionSettings = useMemo(
    () => collectQuestionSettings(survey?.contentItems),
    [survey?.contentItems],
  );

  if (isLoading) {
    return (
      <main className="catalog-detail-page survey-results-page">
        <div className="catalog-detail-container survey-results-container">
          <BackButton fallbackTo="/resultados" className="catalog-detail-back" />
          <Breadcrumbs
            className="catalog-detail-breadcrumb"
            items={[{ label: 'PANEL', to: '/panel' }, { label: 'RESULTADOS', to: '/resultados' }, { label: 'ENCUESTA' }]}
          />
          <header className="catalog-detail-page-header">
            <h1>RESULTADOS Y ESTADÍSTICAS</h1>
            <p>Consulta las respuestas agregadas y el análisis ponderado de la encuesta.</p>
          </header>
          <div className="catalog-detail-loading"><Skeleton variant="list" count={4} /></div>
        </div>
      </main>
    );
  }

  if (errorMessage || !survey || !statistics) {
    return (
      <main className="catalog-detail-page survey-results-page">
        <div className="catalog-detail-container survey-results-container">
          <BackButton fallbackTo="/resultados" className="catalog-detail-back" />
          <Breadcrumbs
            className="catalog-detail-breadcrumb"
            items={[{ label: 'PANEL', to: '/panel' }, { label: 'RESULTADOS', to: '/resultados' }, { label: 'ENCUESTA' }]}
          />
          <header className="catalog-detail-page-header">
            <h1>RESULTADOS Y ESTADÍSTICAS</h1>
            <p>Consulta las respuestas agregadas y el análisis ponderado de la encuesta.</p>
          </header>
          <ErrorMessage
            message={errorMessage || 'No se han encontrado los resultados.'}
            onClose={() => navigate('/resultados')}
          />
        </div>
      </main>
    );
  }

  const questions = Array.isArray(statistics.questions) ? statistics.questions : [];

  return (
    <main className="catalog-detail-page survey-results-page">
      <div className="catalog-detail-container survey-results-container">
        <BackButton fallbackTo="/resultados" className="catalog-detail-back" />

        <Breadcrumbs className="catalog-detail-breadcrumb" items={[
          { label: 'PANEL', to: '/panel' },
          { label: 'RESULTADOS', to: '/resultados' },
          { label: survey.title || 'ENCUESTA' },
        ]} />

        <header className="catalog-detail-page-header">
          <h1>RESULTADOS Y ESTADÍSTICAS</h1>
          <p>Consulta las respuestas agregadas y el análisis ponderado de la encuesta.</p>
        </header>

        <article className="catalog-detail-card survey-results-card">
          <header className="catalog-detail-title-bar survey-results-title-bar">
            <div className="survey-results-title-heading">
              <span className="catalog-detail-title-icon" aria-hidden="true"><MdAssessment /></span>
              <h2>{survey.title || 'Encuesta sin título'}</h2>
            </div>
            <span className={`table-badge table-badge-${String(survey.lifecycleStatus || 'DRAFT').toLowerCase()}`}>
              {survey.lifecycleStatus === 'OPEN' ? 'ABIERTA' : survey.lifecycleStatus === 'CLOSED' ? 'CERRADA' : 'BORRADOR'}
            </span>
          </header>

          <div className="catalog-detail-body survey-results-body">
            <p className="catalog-detail-description">
              {survey.description || 'Esta encuesta no tiene descripción.'}
            </p>

            <dl className="catalog-detail-metadata survey-results-summary" aria-label="Resumen de resultados">
              <div><dt>PARTICIPACIONES</dt><dd>{statistics.participations}</dd></div>
              <div><dt>PREGUNTAS</dt><dd>{questions.length}</dd></div>
              <div><dt>PRIMERA RESPUESTA</dt><dd>{formatDateTime(statistics.firstParticipation)}</dd></div>
              <div><dt>ÚLTIMA RESPUESTA</dt><dd>{formatDateTime(statistics.lastParticipation)}</dd></div>
            </dl>

            <div className="survey-results-participations-action">
              <button
                type="button"
                disabled={statistics.participations === 0}
                onClick={() => navigate(`/encuestas/${surveyId}/resultados/participaciones`)}
              >
                <MdOutlineRemoveRedEye aria-hidden="true" />
                Ver participaciones individuales
              </button>
              <p>Las respuestas se muestran de forma anónima y no pueden modificarse.</p>
            </div>

            <WeightedAnalysis
              score={statistics.globalEvaluationScore}
              hasParticipations={statistics.participations > 0}
            />

            <section className="catalog-detail-structure questions-statistics-section" aria-labelledby="questions-statistics-title">
              <header className="catalog-detail-structure-header">
                <div>
                  <h3 id="questions-statistics-title">ESTADÍSTICAS POR PREGUNTA</h3>
                  <p>Los porcentajes se calculan sobre quienes respondieron cada pregunta; las omisiones se muestran por separado.</p>
                </div>
              </header>

              <div className="catalog-detail-structure-content questions-statistics-content">
                {questions.length === 0 ? (
                  <div className="questions-statistics-empty">Esta encuesta no contiene preguntas.</div>
                ) : (
                  <div className="questions-statistics-list">
                    {questions.map((question, index) => (
                      <QuestionStatistics
                        key={question.questionId}
                        statistics={question}
                        index={index}
                        question={questionSettings.get(question.questionId)}
                      />
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        </article>
      </div>
    </main>
  );
}
