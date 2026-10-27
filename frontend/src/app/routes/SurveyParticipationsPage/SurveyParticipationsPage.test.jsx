import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getSurveyParticipation, getSurveyParticipations } from '../../../api/participationApi.js';
import { getSurveyForResults } from '../../../api/surveysApi.js';
import { SurveyParticipationsPage } from './SurveyParticipationsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/participationApi.js', () => ({
  getSurveyParticipation: vi.fn(),
  getSurveyParticipations: vi.fn(),
}));
vi.mock('../../../api/surveysApi.js', () => ({ getSurveyForResults: vi.fn() }));

function renderPage() {
  renderWithRouter(
    <Routes><Route path="/encuestas/:surveyId/participaciones" element={<SurveyParticipationsPage />} /></Routes>,
    { route: '/encuestas/12/participaciones' },
  );
}

describe('SurveyParticipationsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getSurveyForResults.mockResolvedValue({ id: 12, title: 'Hábitos digitales' });
    getSurveyParticipations.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra un estado vacío y el título de la encuesta', async () => {
    renderPage();
    expect(await screen.findByText('Esta encuesta todavía no tiene participaciones.')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    expect(getSurveyParticipations).toHaveBeenCalledWith('12', expect.objectContaining({ page: 0, size: 10 }));
  });

  it('abre y cierra respuestas individuales sin exponer datos personales', async () => {
    const user = userEvent.setup();
    getSurveyParticipations.mockResolvedValue({
      content: [{ id: 5, submittedAt: '2026-10-01T12:00:00Z' }], totalElements: 1, totalPages: 1,
    });
    getSurveyParticipation.mockResolvedValue({
      id: 5,
      submittedAt: '2026-10-01T12:00:00Z',
      answers: [{ answerId: 1, questionType: 'TEXT', statement: 'Tu opinión', textValue: 'Muy útil' }],
    });
    renderPage();
    const detailToggle = await screen.findByRole('button', { name: 'Ver respuestas' });
    expect(detailToggle).not.toHaveAttribute('aria-controls');
    await user.click(detailToggle);
    expect(await screen.findByText('Muy útil')).toBeVisible();
    expect(document.getElementById(detailToggle.getAttribute('aria-controls'))).toBeVisible();
    expect(getSurveyParticipation).toHaveBeenCalledWith('12', 5);
    await user.click(screen.getByRole('button', { name: 'Cerrar detalle' }));
    expect(screen.queryByText('Muy útil')).not.toBeInTheDocument();
    expect(detailToggle).not.toHaveAttribute('aria-controls');
  });

  it('conserva la lista si falla la carga de un detalle', async () => {
    const user = userEvent.setup();
    getSurveyParticipations.mockResolvedValue({
      content: [{ id: 5, submittedAt: '2026-10-01T12:00:00Z' }], totalElements: 1, totalPages: 1,
    });
    getSurveyParticipation.mockRejectedValue(new Error('Detalle no disponible'));
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Ver respuestas' }));
    expect(await screen.findByText('Detalle no disponible')).toBeVisible();
    expect(screen.getAllByText('Participación #5')).toHaveLength(2);
  });

  it('muestra respuestas numéricas, booleanas, opciones y matrices', async () => {
    const user = userEvent.setup();
    getSurveyParticipations.mockResolvedValue({ content: [{ id: 8, submittedAt: '2026-10-01T12:00:00Z' }], totalElements: 1, totalPages: 1 });
    getSurveyParticipation.mockResolvedValue({ id: 8, answers: [
      { answerId: 1, questionType: 'NUMBER', statement: 'Edad', numericValue: 25 },
      { answerId: 2, questionType: 'BOOLEAN', statement: '¿Conforme?', booleanValue: false, falseLabel: 'En desacuerdo' },
      { answerId: 3, questionType: 'SINGLE_CHOICE', statement: 'Preferencia', selectedOptions: [{ optionId: 3, label: 'Otra', otherText: 'Mi respuesta' }] },
      { answerId: 4, questionType: 'MATRIX_SCALE', statement: 'Valoración', matrixValues: [{ rowId: 1, label: 'Facilidad', numericValue: 4 }] },
    ] });
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Ver respuestas' }));
    expect(await screen.findByText('En desacuerdo')).toBeVisible();
    expect(screen.getByText('25')).toBeVisible();
    expect(screen.getByText('Mi respuesta')).toBeVisible();
    expect(screen.getByText('Facilidad')).toBeVisible();
    expect(screen.getByText('4')).toBeVisible();
  });

  it('muestra errores independientes de la encuesta y del listado', async () => {
    getSurveyForResults.mockRejectedValue(new Error('Encuesta no disponible'));
    getSurveyParticipations.mockRejectedValue(new Error('Listado no disponible'));
    renderPage();
    expect(await screen.findByText('Encuesta no disponible')).toBeVisible();
    expect(await screen.findByText('Listado no disponible')).toBeVisible();
    expect(screen.queryByText('Esta encuesta todavía no tiene participaciones.')).not.toBeInTheDocument();
  });

  it('cambia de página y de tamaño conservando la consulta', async () => {
    const user = userEvent.setup();
    getSurveyParticipations.mockResolvedValue({ content: [{ id: 8, submittedAt: '2026-10-01T12:00:00Z' }], totalElements: 30, totalPages: 3 });
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Página siguiente' }));
    await waitFor(() => expect(getSurveyParticipations).toHaveBeenCalledWith('12', expect.objectContaining({ page: 1, size: 10 })));
    await user.click(screen.getByRole('button', { name: /Participaciones por página:/ }));
    fireEvent.click(screen.getByRole('button', { name: '25 por página' }));
    await waitFor(() => expect(getSurveyParticipations).toHaveBeenCalledWith('12', expect.objectContaining({ page: 0, size: 25 })));
  });
});
