import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getSurveyStatistics } from '../../../api/statisticsApi.js';
import { getSurveyForResults } from '../../../api/surveysApi.js';
import { SurveyResultsPage } from './SurveyResultsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/statisticsApi.js', () => ({ getSurveyStatistics: vi.fn() }));
vi.mock('../../../api/surveysApi.js', () => ({ getSurveyForResults: vi.fn() }));
vi.mock('../../../components/Question/Question.jsx', () => ({ Question: ({ data }) => <p>Pregunta: {data.statement}</p> }));

function renderPage() {
  renderWithRouter(
    <Routes>
      <Route path="/encuestas/:surveyId/resultados" element={<SurveyResultsPage />} />
      <Route path="/encuestas/:surveyId/resultados/participaciones" element={<h1>Participaciones individuales</h1>} />
    </Routes>,
    { route: '/encuestas/12/resultados' },
  );
}

describe('SurveyResultsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getSurveyForResults.mockResolvedValue({ id: 12, title: 'Hábitos digitales', lifecycleStatus: 'OPEN', contentItems: [] });
    getSurveyStatistics.mockResolvedValue({ participations: 0, questions: [], globalEvaluationScore: null });
  });

  it('muestra el estado sin participaciones ni preguntas', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    expect(screen.getByText('Esta encuesta no contiene preguntas.')).toBeVisible();
    expect(screen.getByText('El análisis estará disponible cuando se registre alguna participación.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Ver participaciones individuales' })).toBeDisabled();
  });

  it('presenta la distribución y abre las participaciones de una encuesta respondida', async () => {
    const user = userEvent.setup();
    getSurveyForResults.mockResolvedValue({
      id: 12,
      title: 'Hábitos digitales',
      lifecycleStatus: 'CLOSED',
      contentItems: [{ itemType: 'QUESTION', question: { id: 4, statement: '¿Te gusta?', questionType: 'BOOLEAN' } }],
    });
    getSurveyStatistics.mockResolvedValue({
      participations: 3,
      firstParticipation: '2026-10-01T12:00:00Z',
      lastParticipation: '2026-10-02T12:00:00Z',
      globalEvaluationScore: 35,
      questions: [{
        questionId: 4, questionType: 'BOOLEAN', statement: '¿Te gusta?',
        responseRate: 100, answers: 3, omissions: 0,
        booleanStatistics: { trueCount: 2, truePercentage: 66.7, falseCount: 1, falsePercentage: 33.3 },
      }],
    });
    renderPage();
    expect(await screen.findByRole('heading', { name: '¿Te gusta?' })).toBeVisible();
    expect(screen.getByText('Tendencia positiva')).toBeVisible();
    expect(screen.getByRole('img', { name: /Sí: 66,7 % de las selecciones/ })).toBeVisible();
    const distribution = screen.getByText('Ver distribución por opción');
    expect(distribution.closest('details')).not.toHaveAttribute('open');
    await user.click(distribution);
    expect(distribution.closest('details')).toHaveAttribute('open');
    const questionToggle = screen.getByRole('button', { name: 'Ver pregunta' });
    expect(questionToggle).not.toHaveAttribute('aria-controls');
    await user.click(questionToggle);
    expect(document.getElementById(questionToggle.getAttribute('aria-controls'))).toBeVisible();
    expect(screen.getByText('Pregunta: ¿Te gusta?')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver participaciones individuales' }));
    expect(screen.getByRole('heading', { name: 'Participaciones individuales' })).toBeVisible();
  });

  it('muestra las opciones booleanas aunque todavía no haya respuestas', async () => {
    getSurveyStatistics.mockResolvedValue({
      participations: 0,
      questions: [{ questionId: 4, questionType: 'BOOLEAN', statement: '¿Te gusta?', answers: 0, omissions: 0, responseRate: 0 }],
    });
    renderPage();
    expect(await screen.findByRole('heading', { name: '¿Te gusta?' })).toBeVisible();
    expect(screen.getByRole('img', { name: 'Todavía no hay respuestas para esta distribución' })).toBeVisible();
    expect(screen.getByText('Ver distribución por opción').closest('details')).not.toHaveAttribute('open');
  });

  it('muestra el error cuando las estadísticas no se pueden cargar', async () => {
    getSurveyStatistics.mockRejectedValue(new Error('Servicio no disponible'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
  });
});


describe('SurveyResultsPage: distribuciones y evaluación', () => {
  it.each([[-40, 'Tendencia negativa'], [0, 'Tendencia equilibrada'], [null, 'No hay preguntas evaluables']])('interpreta el índice global %s', async (score, label) => {
    getSurveyForResults.mockResolvedValue({ id: 12, title: 'Resultados', lifecycleStatus: 'DRAFT', contentItems: [] });
    getSurveyStatistics.mockResolvedValue({ participations: 2, globalEvaluationScore: score, questions: [] });
    renderPage();
    expect(await screen.findByText(new RegExp(label))).toBeVisible();
  });
  it('muestra distribuciones numéricas, de opciones y de matriz con evaluación', async () => {
    const user = userEvent.setup();
    const types = ['TEXT', 'NUMBER', 'MULTIPLE_CHOICE', 'MATRIX_SCALE', 'UNKNOWN'];
    getSurveyForResults.mockResolvedValue({ id: 12, title: 'Resultados', lifecycleStatus: 'CLOSED', contentItems: [
      { itemType: 'BLOCK', block: { items: types.map((questionType, index) => ({ itemType: 'QUESTION', isRequired: true, question: { id: index + 1, statement: questionType, questionType, evaluationWeight: index === 1 ? -2 : 2, evaluationDirection: index === 1 ? 'NEGATIVE' : 'CUSTOM' } })) } },
      { itemType: 'BLOCK' },
    ] });
    getSurveyStatistics.mockResolvedValue({ participations: 3, globalEvaluationScore: 0, questions: types.map((questionType, index) => ({
      questionId: index + 1, questionType, statement: questionType, answers: 3, omissions: 0, responseRate: 100, evaluationScore: 2,
      ...(questionType === 'TEXT' ? { textResponses: ['Primera opinión', 'Otra opinión'] } : {}),
      ...(questionType === 'NUMBER' ? { numericStatistics: { average: 2, median: 2, min: 1, max: 3, standardDeviation: 'invalid' }, valueDistribution: [{ value: 1, count: 1, percentage: 33 }] } : {}),
      ...(questionType === 'MULTIPLE_CHOICE' ? { choiceStatistics: [{ optionId: 1, label: 'Opción A', count: 1, percentage: 150 }, { label: 'Opción B', count: 0, percentage: null }] } : {}),
      ...(questionType === 'MATRIX_SCALE' ? { matrixStatistics: [{ rowId: 1, label: 'Rapidez', answers: 1, numericStatistics: { average: 3 }, valueDistribution: [{ value: 3, count: 1, percentage: 100 }] }, { rowId: 2, label: 'Precisión', answers: 2 }] } : {}),
    })) });
    renderPage();
    expect(await screen.findByText('Resultados por fila')).toBeVisible();
    expect(screen.getByText('Primera opinión')).toBeVisible();
    expect(screen.getByText('Otra opinión')).toBeVisible();
    expect(screen.getByText(/la suma puede superar/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Opción A: 100 % de las selecciones/ })).toBeVisible();
    expect(screen.getAllByText('Ver distribución por opción')).toHaveLength(3);
    const toggle = screen.getAllByRole('button', { name: 'Ver pregunta' })[0];
    await user.click(toggle);
    await user.click(screen.getByRole('button', { name: 'Ocultar pregunta' }));
    expect(screen.queryByText('Pregunta: TEXT')).not.toBeInTheDocument();
  });
});
