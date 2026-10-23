import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getResultSurveys } from '../../../api/surveysApi.js';
import { ResultsPage } from './ResultsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/surveysApi.js', () => ({ getResultSurveys: vi.fn() }));

describe('ResultsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getResultSurveys.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra el estado vacío cuando no hay respuestas', async () => {
    renderWithRouter(<ResultsPage />);
    expect(await screen.findByRole('heading', { name: 'No hay encuestas con respuestas' })).toBeVisible();
    expect(getResultSurveys).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 9, sortOrder: 'NEWEST' }));
  });

  it('permite navegar a los resultados de una encuesta', async () => {
    const user = userEvent.setup();
    getResultSurveys.mockResolvedValue({
      content: [{ id: 12, title: 'Hábitos digitales', lifecycleStatus: 'OPEN', responses: 7 }],
      totalElements: 1,
      totalPages: 1,
    });
    renderWithRouter(
      <Routes>
        <Route path="/" element={<ResultsPage />} />
        <Route path="/encuestas/:id/resultados" element={<h1>Detalle de resultados</h1>} />
      </Routes>,
    );
    expect(await screen.findByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver resultados' }));
    expect(screen.getByRole('heading', { name: 'Detalle de resultados' })).toBeVisible();
  });

  it('muestra el fallo de carga', async () => {
    getResultSurveys.mockRejectedValueOnce(new Error('No se pudo consultar'));
    renderWithRouter(<ResultsPage />);
    expect(await screen.findByText('No se pudo consultar')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'No se han podido cargar las encuestas' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'No hay encuestas con respuestas' })).not.toBeInTheDocument();
  });

  it('permite reintentar la carga después de un error', async () => {
    const user = userEvent.setup();
    getResultSurveys.mockRejectedValueOnce(new Error('No se pudo consultar'));
    renderWithRouter(<ResultsPage />);

    await user.click(await screen.findByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { name: 'No hay encuestas con respuestas' })).toBeVisible();
    expect(getResultSurveys).toHaveBeenCalledTimes(2);
  });
});
