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
    const questionToggle = screen.getByRole('button', { name: 'Ver pregunta' });
    expect(questionToggle).not.toHaveAttribute('aria-controls');
    await user.click(questionToggle);
    expect(document.getElementById(questionToggle.getAttribute('aria-controls'))).toBeVisible();
    expect(screen.getByText('Pregunta: ¿Te gusta?')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver participaciones individuales' }));
    expect(screen.getByRole('heading', { name: 'Participaciones individuales' })).toBeVisible();
  });

  it('muestra el error cuando las estadísticas no se pueden cargar', async () => {
    getSurveyStatistics.mockRejectedValue(new Error('Servicio no disponible'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
  });
});
