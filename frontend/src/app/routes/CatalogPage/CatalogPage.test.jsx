import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  deleteCatalogBlock,
  deleteCatalogQuestion,
  getCatalogBlocks,
  getCatalogQuestions,
  getCatalogTemplates,
} from '../../../api/catalogApi.js';
import { CatalogPage } from './CatalogPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/catalogApi.js', () => ({
  deleteCatalogBlock: vi.fn(),
  deleteCatalogQuestion: vi.fn(),
  deleteCatalogTemplate: vi.fn(),
  getCatalogBlocks: vi.fn(),
  getCatalogQuestions: vi.fn(),
  getCatalogTemplates: vi.fn(),
}));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([])) }));

const block = {
  id: 12,
  name: 'Bloque digital',
  description: 'Opiniones sobre tecnología',
  questionCount: 2,
  creatorName: 'Ana',
  createdAt: '2026-10-01T12:00:00Z',
  updatedAt: '2026-10-01T12:00:00Z',
};

function renderPage(role = 'EDITOR', section = 'bloques') {
  renderWithRouter(
    <Routes>
      <Route path="/catalogo/:section" element={<CatalogPage />} />
      <Route path="/catalogo/detalles/bloque/:id" element={<h1>Detalle del bloque</h1>} />
    </Routes>,
    { route: `/catalogo/${section}`, auth: { user: { id: 7, role } } },
  );
}

describe('CatalogPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    const emptyPage = { content: [], totalElements: 0, totalPages: 0 };
    getCatalogBlocks.mockResolvedValue(emptyPage);
    getCatalogQuestions.mockResolvedValue(emptyPage);
    getCatalogTemplates.mockResolvedValue(emptyPage);
  });

  it('muestra al editor el catálogo público con sus acciones de consulta', async () => {
    const user = userEvent.setup();
    getCatalogBlocks.mockResolvedValue({ content: [block], totalElements: 1, totalPages: 1 });
    renderPage();
    expect(await screen.findByRole('heading', { name: 'BLOQUE DIGITAL' })).toBeVisible();
    expect(getCatalogBlocks).toHaveBeenCalledWith(expect.objectContaining({ mine: false, page: 0 }));
    expect(getCatalogBlocks).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Ver detalles de Bloque digital' }));
    expect(screen.getByRole('heading', { name: 'Detalle del bloque' })).toBeVisible();
  });

  it('permite al administrador borrar un bloque propio tras confirmar', async () => {
    const user = userEvent.setup();
    getCatalogBlocks.mockImplementation(({ mine }) => Promise.resolve(mine
      ? { content: [block], totalElements: 1, totalPages: 1 }
      : { content: [], totalElements: 0, totalPages: 0 }));
    deleteCatalogBlock.mockResolvedValue(null);
    renderPage('ADMIN');
    const ownSection = screen.getByRole('region', { name: 'MIS BLOQUES' });
    await within(ownSection).findByRole('heading', { name: 'BLOQUE DIGITAL' });
    await user.click(within(ownSection).getByRole('button', { name: 'Opciones de Bloque digital' }));
    await user.click(within(ownSection).getByRole('menuitem', { name: 'Borrar' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar bloque' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));
    expect(deleteCatalogBlock).toHaveBeenCalledWith(12);
    expect(await within(ownSection).findByText('No se encontraron bloques disponibles.')).toBeVisible();
  });

  it('redirecciona una sección de catálogo no reconocida', async () => {
    renderPage('EDITOR', 'desconocida');
    expect(await screen.findByRole('tab', { name: 'Bloques' })).toHaveAttribute('aria-selected', 'true');
  });

  it('permite cambiar de sección y buscar en el catálogo de preguntas', async () => {
    const user = userEvent.setup();
    getCatalogQuestions.mockResolvedValue({ content: [{ id: 6, statement: '¿Edad?', questionType: 'NUMBER' }], totalElements: 1, totalPages: 1 });
    renderPage('EDITOR', 'preguntas');
    expect(await screen.findByRole('heading', { name: '¿Edad?' })).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: 'Introduce una palabra para buscar en el catálogo' }), 'edad');
    await vi.waitFor(() => expect(getCatalogQuestions).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'edad' })));
    await user.click(screen.getByRole('tab', { name: 'Plantillas' }));
    expect(screen.getByRole('tab', { name: 'Plantillas' })).toHaveAttribute('aria-selected', 'true');
    expect(getCatalogTemplates).toHaveBeenCalled();
  });

  it('muestra el menú de creación para administradores', async () => {
    const user = userEvent.setup();
    renderPage('ADMIN');
    await user.click(screen.getByRole('button', { name: 'Crear elemento' }));
    expect(screen.getByRole('link', { name: 'Plantilla' })).toHaveAttribute('href', '/catalogo/crear/plantilla');
    expect(screen.getByRole('link', { name: 'Pregunta fija' })).toHaveAttribute('href', '/catalogo/crear/pregunta');
  });

  it('permite al superadministrador retirar una pregunta de la lista pública', async () => {
    const user = userEvent.setup();
    getCatalogQuestions.mockImplementation(({ mine }) => Promise.resolve(mine
      ? { content: [], totalElements: 0, totalPages: 0 }
      : { content: [{ id: 6, statement: '¿Edad?', questionType: 'NUMBER' }], totalElements: 1, totalPages: 1 }));
    deleteCatalogQuestion.mockResolvedValue(null);
    renderPage('SUPERADMIN', 'preguntas');
    const publicSection = screen.getByRole('region', { name: 'LISTA PÚBLICA' });
    await within(publicSection).findByRole('heading', { name: '¿Edad?' });
    await user.click(within(publicSection).getByRole('button', { name: 'Opciones de ¿Edad?' }));
    await user.click(within(publicSection).getByRole('menuitem', { name: 'Borrar' }));
    await user.click(within(screen.getByRole('dialog', { name: 'Eliminar pregunta' })).getByRole('button', { name: 'Eliminar' }));
    expect(deleteCatalogQuestion).toHaveBeenCalledWith(6);
  });
});
