import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getAdminDashboard } from '../../../api/dashboardApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import { getAdminSurveys, getMySurveys } from '../../../api/surveysApi.js';
import { PanelPage } from './PanelPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/dashboardApi.js', () => ({ getAdminDashboard: vi.fn() }));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn() }));
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
    getCategories.mockResolvedValue([
      { code: 'ORDENADORES', name: 'Ordenadores' },
      { code: 'TELEFONO_INTELIGENTE', name: 'Teléfono inteligente' },
    ]);
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

  it('aplica acceso, estado y varias categorías desde el filtro del editor', async () => {
    const user = userEvent.setup();
    renderPage('EDITOR');
    expect(getCategories).toHaveBeenCalledWith({ surveyOnly: true });

    await user.click(await screen.findByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filters).getByRole('checkbox', { name: 'Públicas' }));
    await user.selectOptions(within(filters).getByRole('combobox', { name: 'Estado de la encuesta' }), 'OPEN');
    await user.click(within(filters).getByLabelText('Seleccionar categoría'));
    const categoryOptions = within(filters).getByRole('group', { name: 'Categoría' });
    await user.click(within(categoryOptions).getByRole('checkbox', { name: 'Ordenadores' }));
    await user.click(within(categoryOptions).getByRole('checkbox', { name: 'Teléfono inteligente' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));

    await vi.waitFor(() => expect(getMySurveys).toHaveBeenCalledWith(expect.objectContaining({
      lifecycleStatus: 'OPEN',
      accessModes: ['PASSWORD_PROTECTED', 'AUTHENTICATED'],
      categories: ['ORDENADORES', 'TELEFONO_INTELIGENTE'],
      page: 0,
    })));

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(within(screen.getByRole('group', { name: 'Filtrar' })).getByRole('button', { name: 'Borrar filtros' }));
    await vi.waitFor(() => expect(getMySurveys).toHaveBeenCalledWith(expect.objectContaining({
      lifecycleStatus: 'ALL',
      accessModes: ['PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED'],
      categories: [],
    })));
  });

  it('muestra un aviso al desmarcar todos los tipos de acceso', async () => {
    const user = userEvent.setup();
    renderPage('EDITOR');
    await user.click(await screen.findByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filters).getByRole('checkbox', { name: 'Públicas' }));
    await user.click(within(filters).getByRole('checkbox', { name: 'Con contraseña' }));
    await user.click(within(filters).getByRole('checkbox', { name: 'Para usuarios registrados' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('Selecciona al menos un tipo de encuesta para ver los resultados.')).toBeVisible();
    expect(getMySurveys).toHaveBeenCalledOnce();
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
    expect(getAdminSurveys).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 5 }));
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
