import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CatalogSidebar } from './CatalogSidebar.jsx';
import { getCatalogBlocksForBuilder, getCatalogQuestionsForBuilder } from '../../api/catalogApi.js';

vi.mock('../../api/catalogApi.js', () => ({
  getCatalogBlocksForBuilder: vi.fn(),
  getCatalogQuestionsForBuilder: vi.fn(),
}));

const question = { id: 4, statement: '¿Cómo estás?', questionType: 'TEXT' };
const block = { id: 7, name: 'Hábitos', questionCount: 2 };

function renderSidebar(props = {}) {
  render(<MemoryRouter><CatalogSidebar embedded onAddQuestion={vi.fn()} onAddBlock={vi.fn()} {...props} /></MemoryRouter>);
}

describe('CatalogSidebar', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCatalogQuestionsForBuilder.mockResolvedValue({ content: [question], totalElements: 1 });
    getCatalogBlocksForBuilder.mockResolvedValue({ content: [block], totalElements: 1 });
  });

  it('carga preguntas y bloques y los añade al constructor', async () => {
    const user = userEvent.setup();
    const onAddQuestion = vi.fn();
    const onAddBlock = vi.fn();
    renderSidebar({ onAddQuestion, onAddBlock });
    expect(await screen.findByRole('heading', { name: '¿Cómo estás?' })).toBeVisible();
    expect(await screen.findByRole('heading', { name: 'HÁBITOS' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Añadir pregunta: ¿Cómo estás?' }));
    await user.click(screen.getByRole('button', { name: 'Añadir bloque: Hábitos' }));
    expect(onAddQuestion).toHaveBeenCalledWith(question);
    expect(onAddBlock).toHaveBeenCalledWith(block);
  });

  it('filtra los elementos cargados por su texto', async () => {
    const user = userEvent.setup();
    renderSidebar();
    await screen.findByRole('heading', { name: '¿Cómo estás?' });
    await user.type(screen.getByRole('searchbox', { name: 'Buscar preguntas fijas' }), 'otra');
    expect(screen.queryByRole('heading', { name: '¿Cómo estás?' })).not.toBeInTheDocument();
    expect(screen.getByText(/No se han encontrado preguntas/)).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: 'Buscar bloques de preguntas' }), 'otro');
    expect(screen.queryByRole('heading', { name: 'HÁBITOS' })).not.toBeInTheDocument();
  });

  it('permite reintentar la carga fallida de preguntas', async () => {
    const user = userEvent.setup();
    getCatalogQuestionsForBuilder.mockRejectedValueOnce(new Error('Red caída'));
    renderSidebar({ showBlocks: false });
    expect(await screen.findByRole('alert')).toHaveTextContent('Red caída');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { name: '¿Cómo estás?' })).toBeVisible();
    expect(getCatalogQuestionsForBuilder).toHaveBeenCalledTimes(2);
  });

  it('carga páginas adicionales de preguntas', async () => {
    const user = userEvent.setup();
    getCatalogQuestionsForBuilder
      .mockResolvedValueOnce({ content: [question], totalElements: 2 })
      .mockResolvedValueOnce({ content: [{ id: 5, statement: '¿Edad?', questionType: 'NUMBER' }], totalElements: 2 });
    renderSidebar({ showBlocks: false });
    await screen.findByRole('heading', { name: '¿Cómo estás?' });
    await user.click(screen.getByRole('button', { name: 'Ver más preguntas' }));
    expect(await screen.findByRole('heading', { name: '¿Edad?' })).toBeVisible();
    await waitFor(() => expect(getCatalogQuestionsForBuilder).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })));
  });

  it('muestra el error al añadir un elemento sin perder la lista', async () => {
    const user = userEvent.setup();
    const onAddQuestion = vi.fn().mockRejectedValue(new Error('No se puede añadir'));
    renderSidebar({ showBlocks: false, onAddQuestion });
    await user.click(await screen.findByRole('button', { name: 'Añadir pregunta: ¿Cómo estás?' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se puede añadir');
    expect(screen.getByRole('heading', { name: '¿Cómo estás?' })).toBeVisible();
  });

  it('permite abrir y cerrar el panel lateral con los controles accesibles', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><CatalogSidebar showBlocks={false} defaultOpen={false} /></MemoryRouter>);
    await user.click(screen.getByRole('button', { name: 'Abrir catálogo de elementos' }));
    expect(screen.getByRole('heading', { name: 'CATÁLOGO DE ELEMENTOS' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar catálogo de elementos' }));
    expect(screen.getByRole('button', { name: 'Abrir catálogo de elementos' })).toHaveAttribute('aria-expanded', 'false');
  });

  it('filtra por tipo de pregunta respetando los tipos admitidos', async () => {
    const user = userEvent.setup();
    getCatalogQuestionsForBuilder.mockResolvedValue({ content: [question, { id: 5, statement: '¿Edad?', questionType: 'NUMBER' }], totalElements: 2 });
    renderSidebar({ allowedQuestionTypes: ['TEXT', 'NUMBER'], showBlocks: false });
    await screen.findByRole('heading', { name: '¿Edad?' });
    await user.click(screen.getByRole('button', { name: /Filtrar preguntas por tipo:/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Numérica' }));
    expect(screen.getByRole('heading', { name: '¿Edad?' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: '¿Cómo estás?' })).not.toBeInTheDocument();
  });

  it('reintenta los bloques y carga una segunda página', async () => {
    const user = userEvent.setup();
    getCatalogBlocksForBuilder.mockRejectedValueOnce(new Error('Bloques no disponibles'))
      .mockResolvedValueOnce({ content: [block], totalElements: 2 })
      .mockResolvedValueOnce({ content: [{ id: 8, name: 'Salud', questionCount: 1 }], totalElements: 2 });
    renderSidebar({ showQuestions: false });
    expect(await screen.findByRole('alert')).toHaveTextContent('Bloques no disponibles');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { name: 'HÁBITOS' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver más bloques' }));
    expect(await screen.findByRole('heading', { name: 'SALUD' })).toBeVisible();
    expect(getCatalogBlocksForBuilder).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
  });

  it('envía los datos del elemento al iniciar un arrastre', async () => {
    const { container } = render(<MemoryRouter><CatalogSidebar embedded showBlocks={false} onAddQuestion={vi.fn()} /></MemoryRouter>);
    await screen.findByRole('heading', { name: '¿Cómo estás?' });
    const data = new Map();
    const dataTransfer = { setData: (type, value) => data.set(type, value), effectAllowed: '' };
    const article = container.querySelector('.catalog-item');
    fireEvent.dragStart(article, { dataTransfer });
    expect(JSON.parse(data.get('application/x-formquid-catalog-item'))).toEqual({ type: 'CATALOG_QUESTION', question });
    expect(dataTransfer.effectAllowed).toBe('copy');
    fireEvent.dragEnd(article);
    expect(article).not.toHaveClass('dragging');
  });
});
