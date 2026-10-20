import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getAdminDashboard } from '../../../api/dashboardApi.js';
import { getAdminSurveys, getMySurveys } from '../../../api/surveysApi.js';
import { PanelPage } from './PanelPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/dashboardApi.js', () => ({ getAdminDashboard: vi.fn() }));
vi.mock('../../../api/surveysApi.js', () => ({ getAdminSurveys: vi.fn(), getMySurveys: vi.fn() }));
vi.mock('../../../components/SurveyOriginModal/SurveyOriginModal.jsx', () => ({
  SurveyOriginModal: ({ isOpen, onSelect }) => isOpen && (
    <div role="dialog" aria-label="Origen de la encuesta">
      <button type="button" onClick={() => onSelect('BLANK')}>Crear desde cero</button>
    </div>
  ),
}));

function CreateDestination() {
  const location = useLocation();
  return <h1>Origen elegido: {location.state?.surveyOrigin}</h1>;
}

function renderPage(role) {
  renderWithRouter(<PanelPage />, { auth: { user: { id: 7, role } } });
}

describe('PanelPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getMySurveys.mockResolvedValue({ content: [], totalPages: 1 });
    getAdminDashboard.mockResolvedValue({ pendingElementRequests: 2, pendingRegistrationRequests: 3, surveyCount: 4 });
    getAdminSurveys.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('muestra las encuestas del editor y abre la elección de origen', async () => {
    const user = userEvent.setup();
    getMySurveys.mockResolvedValue({
      content: [{ id: 12, title: 'Hábitos digitales', lifecycleStatus: 'OPEN' }], totalPages: 1,
    });
    renderWithRouter(
      <Routes>
        <Route path="/" element={<PanelPage />} />
        <Route path="/encuestas/crear" element={<CreateDestination />} />
      </Routes>,
      { auth: { user: { id: 7, role: 'EDITOR' } } },
    );
    expect(await screen.findByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    expect(getMySurveys).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 9 }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    const dialog = screen.getByRole('dialog', { name: 'Origen de la encuesta' });
    await user.click(within(dialog).getByRole('button', { name: 'Crear desde cero' }));
    expect(screen.getByRole('heading', { name: 'Origen elegido: BLANK' })).toBeVisible();
  });

  it('presenta los contadores y el resumen al administrador', async () => {
    getAdminSurveys.mockResolvedValue({
      content: [{ id: 12, title: 'Hábitos digitales', editorName: 'Ana', lifecycleStatus: 'OPEN', updatedAt: '2026-10-01' }],
      totalElements: 1,
      totalPages: 1,
    });
    renderPage('ADMIN');
    expect(await screen.findByRole('heading', { name: 'PANEL DE ADMINISTRADOR' })).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Ver detalles de Hábitos digitales' })).toBeVisible();
    expect(getAdminDashboard).toHaveBeenCalledOnce();
    expect(getAdminSurveys).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 10 }));
  });

  it('muestra gestiones exclusivas del superadministrador', async () => {
    renderPage('SUPERADMIN');
    expect(await screen.findByRole('heading', { name: 'PANEL DE SUPERADMINISTRADOR' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'OTRAS GESTIONES' })).toBeVisible();
    expect(screen.getByRole('link', { name: /Ver usuarios/ })).toHaveAttribute('href', '/superadmin/usuarios');
  });

  it('informa del fallo de carga del resumen sin ocultar el panel', async () => {
    getAdminSurveys.mockRejectedValue(new Error('Resumen no disponible'));
    renderPage('ADMIN');
    expect(await screen.findByRole('alert')).toHaveTextContent('Resumen no disponible');
    expect(screen.getByRole('heading', { name: 'PANEL DE ADMINISTRADOR' })).toBeVisible();
  });
});
