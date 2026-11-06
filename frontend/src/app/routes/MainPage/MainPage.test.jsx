import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCategories } from '../../../api/categoriesApi.js';
import { getPublicSurveys, getRegisteredSurveys } from '../../../api/surveysApi.js';
import { renderWithRouter } from '../../../testing/render.jsx';
import { MainPage } from './MainPage.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  getPublicSurveys: vi.fn(),
  getRegisteredSurveys: vi.fn(),
}));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn() }));

const emptyPage = { content: [], totalElements: 0, totalPages: 0 };
const survey = {
  id: 1,
  publicCode: 'ABCDEF234567',
  title: 'Vida digital',
  accessMode: 'PUBLIC',
  tags: ['infancia', 'tecnologia'],
};

describe('MainPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getPublicSurveys.mockResolvedValue(emptyPage);
    getRegisteredSurveys.mockResolvedValue(emptyPage);
    getCategories.mockResolvedValue([{ code: 'ORDENADORES', name: 'Ordenadores' }, { code: 'TELEFONO_INTELIGENTE', name: 'Teléfono inteligente' }]);
  });

  it('muestra las secciones públicas y oculta las de cuenta a visitantes', async () => {
    renderWithRouter(<MainPage />);
    expect(screen.getByRole('heading', { name: /La tecnología/ })).toBeVisible();
    expect(await screen.findByText('No hay encuestas públicas disponibles.')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Encuestas para usuarios registrados' })).not.toBeInTheDocument();
    expect(getRegisteredSurveys).not.toHaveBeenCalled();
  });

  it('presenta una encuesta pública enlazada por su código', async () => {
    getPublicSurveys.mockImplementation(({ accessMode }) => Promise.resolve({
      content: accessMode === 'PUBLIC' ? [survey] : [],
      totalElements: accessMode === 'PUBLIC' ? 1 : 0,
      totalPages: 1,
    }));
    renderWithRouter(<MainPage />);
    const publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByRole('heading', { name: survey.title })).toBeVisible();
    expect(within(publicSection).getByText('infancia')).toBeVisible();
    expect(within(publicSection).getByText('tecnologia')).toBeVisible();
    expect(within(publicSection).getByRole('link', { name: `Acceder a la encuesta: ${survey.title}` })).toHaveAttribute('href', '/ABCDEF234567');
  });

  it('usa la fuente de encuestas registradas para usuarios autenticados', async () => {
    renderWithRouter(<MainPage />, { auth: { isAuthenticated: true } });
    expect(await screen.findByRole('heading', { name: 'Encuestas para usuarios registrados' })).toBeVisible();
    expect(getRegisteredSurveys).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'AUTHENTICATED' }));
    expect(getPublicSurveys).not.toHaveBeenCalled();
  });

  it('combina categorías y etiqueta en el botón de filtrar', async () => {
    const user = userEvent.setup();
    renderWithRouter(<MainPage />);
    expect(getCategories).toHaveBeenCalledWith({ surveyOnly: true });

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filters = screen.getByRole('group', { name: 'Filtrar' });
    expect(within(filters).getByRole('searchbox', { name: 'Etiqueta' }).compareDocumentPosition(
      within(filters).getByLabelText('Seleccionar categoría'),
    ) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy();
    await user.type(within(filters).getByRole('searchbox', { name: 'Etiqueta' }), '  infancia  ');
    await user.click(within(filters).getByLabelText('Seleccionar categoría'));
    const categoryOptions = within(filters).getByRole('group', { name: 'Categoría' });
    await user.click(await within(categoryOptions).findByRole('checkbox', { name: 'Ordenadores' }));
    await user.click(within(categoryOptions).getByRole('checkbox', { name: 'Teléfono inteligente' }));
    await user.click(within(filters).getByRole('button', { name: 'Aplicar' }));
    expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ tag: 'infancia', categories: ['ORDENADORES', 'TELEFONO_INTELIGENTE'], accessMode: 'PUBLIC', page: 0 }));
    expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ tag: 'infancia', accessMode: 'PASSWORD_PROTECTED', page: 0 }));

    await user.type(screen.getByRole('searchbox', { name: 'Buscar encuestas por nombre, descripción o etiqueta' }), 'infancia');
    expect(await screen.findByRole('searchbox', { name: 'Buscar encuestas por nombre, descripción o etiqueta' })).toHaveValue('infancia');
    await vi.waitFor(() => expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ search: 'infancia', categories: ['ORDENADORES', 'TELEFONO_INTELIGENTE'] })));

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(within(screen.getByRole('group', { name: 'Filtrar' })).getByRole('button', { name: 'Borrar filtros' }));
    expect(screen.getByRole('searchbox', { name: 'Buscar encuestas por nombre, descripción o etiqueta' })).toHaveValue('');
    await vi.waitFor(() => expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ search: '', tag: '', categories: [] })));
  });

  it('muestra dos encuestas por página al reducir el ancho y recupera el tamaño normal al ampliarlo', async () => {
    const user = userEvent.setup();
    const listeners = new Set();
    let compact = false;
    vi.spyOn(window, 'matchMedia').mockImplementation((query) => ({
      matches: query === '(max-width: 900px)' && compact,
      media: query,
      addEventListener: (event, listener) => {
        if (query === '(max-width: 900px)' && event === 'change') listeners.add(listener);
      },
      removeEventListener: (event, listener) => {
        if (query === '(max-width: 900px)' && event === 'change') listeners.delete(listener);
      },
    }));
    const surveys = Array.from({ length: 3 }, (_, index) => ({
      ...survey,
      publicCode: `ENCUESTA000${index + 1}`,
      title: `Encuesta ${index + 1}`,
    }));
    getPublicSurveys.mockImplementation(({ accessMode, page, size }) => Promise.resolve(
      accessMode === 'PUBLIC'
        ? { content: surveys.slice(page * size, (page + 1) * size), totalElements: 3, totalPages: Math.ceil(3 / size) }
        : emptyPage,
    ));

    renderWithRouter(<MainPage />);
    let publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByRole('heading', { name: 'Encuesta 3' })).toBeVisible();
    expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'PUBLIC', page: 0, size: 9 }));

    compact = true;
    act(() => listeners.forEach((listener) => listener({ matches: true })));
    publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByText('Mostrando 1 a 2 de 3 encuestas')).toBeVisible();
    expect(within(publicSection).queryByRole('heading', { name: 'Encuesta 3' })).not.toBeInTheDocument();
    expect(within(publicSection).queryByRole('button', { name: /^Encuestas por página en encuestas públicas:/ })).not.toBeInTheDocument();
    await user.click(within(publicSection).getByRole('button', { name: 'Página 2' }));
    expect(await within(publicSection).findByRole('heading', { name: 'Encuesta 3' })).toBeVisible();
    expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'PUBLIC', page: 1, size: 2 }));

    compact = false;
    act(() => listeners.forEach((listener) => listener({ matches: false })));
    publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByText('Mostrando 1 a 3 de 3 encuestas')).toBeVisible();
    expect(getPublicSurveys).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'PUBLIC', page: 0, size: 9 }));
  });

  it.each([['página', false], ['página', true], ['tamaño', false], ['tamaño', true]])('recupera el foco al cambiar %s sin interrumpir al usuario: mueve el foco=%s', async (change, movesFocus) => {
    const user = userEvent.setup();
    let resolveNextPage;
    getPublicSurveys.mockImplementation(({ accessMode, page, size }) => {
      if (accessMode !== 'PUBLIC') return Promise.resolve(emptyPage);
      if (page === 0 && size === 9) return Promise.resolve({ content: [survey], totalElements: 10, totalPages: 2 });
      return new Promise((resolve) => { resolveNextPage = resolve; });
    });
    renderWithRouter(<MainPage />);
    const publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    await within(publicSection).findByRole('button', { name: 'Página siguiente' });
    if (change === 'página') {
      within(publicSection).getByRole('button', { name: 'Página siguiente' }).focus();
      await user.keyboard('{Enter}');
    } else {
      within(publicSection).getByRole('button', { name: /^Encuestas por página/ }).focus();
      await user.keyboard('{ArrowDown}{Home}{Enter}');
    }
    expect(await within(publicSection).findByRole('status')).toHaveTextContent('Cargando encuestas públicas');
    const search = screen.getByRole('searchbox', { name: 'Buscar encuestas por nombre, descripción o etiqueta' });
    if (movesFocus) await user.click(search);

    await act(async () => resolveNextPage({
      content: [{ ...survey, title: 'Segunda página' }], totalElements: 10, totalPages: 2,
    }));
    expect(await within(publicSection).findByRole('heading', { name: 'Segunda página' })).toBeVisible();
    expect(movesFocus ? search : within(publicSection).getByRole('heading', { name: 'Encuestas públicas' })).toHaveFocus();
    if (!movesFocus) {
      await user.tab();
      expect(within(publicSection).getByRole('link', { name: 'Acceder a la encuesta: Segunda página' })).toHaveFocus();
    }
  });

  it('permite reintentar una sección que falla al cargar', async () => {
    const user = userEvent.setup();
    getPublicSurveys.mockImplementation(({ accessMode }) => {
      if (accessMode !== 'PUBLIC') return Promise.resolve(emptyPage);
      return getPublicSurveys.mock.calls.filter(([options]) => options.accessMode === 'PUBLIC').length === 1
        ? Promise.reject(new Error('Sin red'))
        : Promise.resolve({ content: [survey], totalElements: 1, totalPages: 1 });
    });
    renderWithRouter(<MainPage />);
    const publicSection = screen.getByRole('region', { name: 'Encuestas públicas' });
    expect(await within(publicSection).findByRole('alert')).toHaveTextContent('No hemos podido conectar');
    await user.click(within(publicSection).getByRole('button', { name: 'Reintentar' }));
    expect(await within(publicSection).findByRole('heading', { name: survey.title })).toBeVisible();
  });
});
