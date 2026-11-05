import { fireEvent, screen, within } from '@testing-library/react';
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
import { getCategories } from '../../../api/categoriesApi.js';
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
    getCategories.mockResolvedValue([
      { code: 'ORDENADORES', name: 'Ordenadores' },
      { code: 'LIBROS_TEXTO_DIGITALES', name: 'Libros digitales' },
    ]);
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

  it.each([
    ['bloques', getCatalogBlocks],
    ['plantillas', getCatalogTemplates],
    ['preguntas', getCatalogQuestions],
  ])('permite ordenar alfabéticamente %s en mis elementos y la lista pública', async (section, load) => {
    const user = userEvent.setup();
    renderPage('ADMIN', section);
    await vi.waitFor(() => expect(load).toHaveBeenCalledTimes(2));

    await user.click(screen.getByRole('button', { name: 'Ordenar catálogo: Más reciente' }));
    const sortMenu = screen.getByRole('group', { name: 'Ordenar catálogo' });
    fireEvent.click(within(sortMenu).getByRole('button', { name: 'Alfabético (A–Z)' }));
    expect(screen.getByRole('button', { name: 'Ordenar catálogo: Alfabético (A–Z)' })).toBeVisible();

    await vi.waitFor(() => {
      expect(load).toHaveBeenCalledWith(expect.objectContaining({ mine: true, page: 0, sortOrder: 'ALPHABETICAL' }));
      expect(load).toHaveBeenCalledWith(expect.objectContaining({ mine: false, page: 0, sortOrder: 'ALPHABETICAL' }));
    });
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

  it.each([null, {}])('tolera respuestas incompletas del catálogo %j', async response => {
    getCatalogBlocks.mockResolvedValue(response);
    renderPage();
    expect(await screen.findByRole('heading', { name: /CATÁLOGO/ })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'BLOQUE DIGITAL' })).not.toBeInTheDocument();
  });
});


describe('Categorías del catálogo', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    const emptyPage = { content: [], totalElements: 0, totalPages: 0 };
    getCatalogBlocks.mockResolvedValue(emptyPage);
    getCatalogQuestions.mockResolvedValue(emptyPage);
    getCatalogTemplates.mockResolvedValue(emptyPage);
  });

  it.each([
    ['bloques', getCatalogBlocks, 'name'],
    ['plantillas', getCatalogTemplates, 'title'],
    ['preguntas', getCatalogQuestions, 'statement'],
  ])('muestra todas las categorías en %s y las identifica en cada fila', async (section, load, titleKey) => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([
      { code: 'ORDENADORES', name: 'Ordenadores' },
      { code: 'LIBROS_TEXTO_DIGITALES', name: 'Libros digitales' },
    ]);
    load.mockResolvedValue({ content: [
      { ...block, id: 12, [titleKey]: 'Primer elemento', category: 'ORDENADORES' },
      { ...block, id: 13, [titleKey]: 'Segundo elemento', category: 'LIBROS_TEXTO_DIGITALES' },
    ], totalElements: 2, totalPages: 1 });
    renderPage('ADMIN', section);
    const list = screen.getByRole('region', { name: 'LISTA PÚBLICA' });
    expect(await within(list).findByText('Ordenadores')).toBeVisible();
    expect(within(list).getByText('Libros digitales')).toBeVisible();
    expect(within(list).getByText('Todas las categorías')).toBeVisible();
    expect(screen.queryByLabelText(/^Categoría/)).not.toBeInTheDocument();
    expect(load).toHaveBeenCalledWith(expect.objectContaining({ mine: false }));
    expect(load).toHaveBeenCalledWith(expect.objectContaining({ mine: true }));
    await user.type(screen.getByRole('searchbox', { name: 'Introduce una palabra para buscar en el catálogo' }), 'elemento');
    await vi.waitFor(() => expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'elemento' })));
    for (const [options] of load.mock.calls) expect(options.category).toBeUndefined();
  });

  it.each([
    ['bloques', getCatalogBlocks, 'name'],
    ['plantillas', getCatalogTemplates, 'title'],
    ['preguntas', getCatalogQuestions, 'statement'],
  ])('combina la búsqueda y la categoría en %s y borra solo el filtro', async (section, load, titleKey) => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([
      { code: 'ORDENADORES', name: 'Ordenadores' },
      { code: 'LIBROS_TEXTO_DIGITALES', name: 'Libros digitales' },
    ]);
    const items = [
      { ...block, id: 12, [titleKey]: 'Primer elemento', category: 'ORDENADORES' },
      { ...block, id: 13, [titleKey]: 'Segundo elemento', category: 'LIBROS_TEXTO_DIGITALES' },
    ];
    load.mockImplementation(({ category }) => {
      const content = category ? items.filter((item) => item.category === category) : items;
      return Promise.resolve({ content, totalElements: content.length, totalPages: 1 });
    });
    renderPage('ADMIN', section);
    const list = screen.getByRole('region', { name: 'LISTA PÚBLICA' });
    expect(await within(list).findByText('Libros digitales')).toBeVisible();
    const search = screen.getByRole('searchbox', { name: 'Introduce una palabra para buscar en el catálogo' });
    await user.type(search, 'elemento');
    await vi.waitFor(() => expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'elemento' })));
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filter = screen.getByRole('group', { name: 'Filtrar' });
    expect(within(filter).queryByText('Campos de búsqueda')).not.toBeInTheDocument();
    await user.selectOptions(within(filter).getByRole('combobox', { name: 'Categoría' }), 'ORDENADORES');
    await user.click(within(filter).getByRole('button', { name: 'Aplicar' }));
    expect((await within(list).findAllByText('Ordenadores')).length).toBeGreaterThan(0);
    expect(within(list).queryByText('Libros digitales')).not.toBeInTheDocument();
    expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ category: 'ORDENADORES', search: 'elemento', page: 0 }));
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(within(screen.getByRole('group', { name: 'Filtrar' })).getByRole('button', { name: 'Borrar filtros' }));
    expect(await within(list).findByText('Libros digitales')).toBeVisible();
    expect(search).toHaveValue('elemento');
    expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ category: undefined, search: 'elemento' }));
  });

});
