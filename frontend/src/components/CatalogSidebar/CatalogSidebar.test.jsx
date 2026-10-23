import { useState } from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CatalogSidebar } from './CatalogSidebar.jsx';
import { getCatalogBlockById, getCatalogBlocksForBuilder, getCatalogQuestionsForBuilder } from '../../api/catalogApi.js';

vi.mock('../../api/catalogApi.js', () => ({
  getCatalogBlockById: vi.fn(),
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
    getCatalogBlockById.mockResolvedValue({ ...block, description: 'Descripción del bloque', creatorName: 'Ana', contentItems: [{ itemType: 'TEXT', textBody: 'Introducción' }] });
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

  it('espera a la categoría y muestra solo elementos de la categoría seleccionada', async () => {
    getCatalogQuestionsForBuilder.mockResolvedValue({
      content: [
        { ...question, category: 'SALUD' },
        { id: 8, statement: '¿Usas tecnología?', questionType: 'TEXT', category: 'TECNOLOGIA' },
      ],
      totalElements: 2,
    });
    getCatalogBlocksForBuilder.mockResolvedValue({
      content: [
        { ...block, category: 'SALUD' },
        { id: 9, name: 'Tecnología', category: 'TECNOLOGIA', questionCount: 1 },
      ],
      totalElements: 2,
    });
    const props = { embedded: true, categoryFilter: '', onAddQuestion: vi.fn(), onAddBlock: vi.fn() };
    const { rerender } = render(<MemoryRouter><CatalogSidebar {...props} /></MemoryRouter>);

    expect(screen.getByText('Selecciona una categoría para ver las preguntas disponibles.')).toBeVisible();
    expect(getCatalogQuestionsForBuilder).not.toHaveBeenCalled();
    expect(getCatalogBlocksForBuilder).not.toHaveBeenCalled();

    rerender(<MemoryRouter><CatalogSidebar {...props} categoryFilter="SALUD" /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: '¿Cómo estás?' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'HÁBITOS' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: '¿Usas tecnología?' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'TECNOLOGÍA' })).not.toBeInTheDocument();
    expect(getCatalogQuestionsForBuilder).toHaveBeenCalledWith(expect.objectContaining({ category: 'SALUD', page: 0 }));
    expect(getCatalogBlocksForBuilder).toHaveBeenCalledWith(expect.objectContaining({ category: 'SALUD', page: 0 }));

    rerender(<MemoryRouter><CatalogSidebar {...props} categoryFilter="TECNOLOGIA" /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: '¿Usas tecnología?' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'TECNOLOGÍA' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: '¿Cómo estás?' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'HÁBITOS' })).not.toBeInTheDocument();
    expect(getCatalogQuestionsForBuilder).toHaveBeenCalledWith(expect.objectContaining({ category: 'TECNOLOGIA', page: 0 }));
    expect(getCatalogBlocksForBuilder).toHaveBeenCalledWith(expect.objectContaining({ category: 'TECNOLOGIA', page: 0 }));
  });

  it('descarta una respuesta antigua al cambiar de categoría durante la carga', async () => {
    let resolveSalud;
    getCatalogQuestionsForBuilder
      .mockImplementationOnce(() => new Promise((resolve) => { resolveSalud = resolve; }))
      .mockResolvedValueOnce({ content: [{ id: 8, statement: '¿Usas tecnología?', questionType: 'TEXT', category: 'TECNOLOGIA' }], totalElements: 1 });
    const { rerender } = render(<MemoryRouter><CatalogSidebar embedded showBlocks={false} categoryFilter="SALUD" /></MemoryRouter>);
    await waitFor(() => expect(getCatalogQuestionsForBuilder).toHaveBeenCalledTimes(1));

    rerender(<MemoryRouter><CatalogSidebar embedded showBlocks={false} categoryFilter="TECNOLOGIA" /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: '¿Usas tecnología?' })).toBeVisible();
    await act(async () => resolveSalud({ content: [{ ...question, category: 'SALUD' }], totalElements: 1 }));

    expect(screen.getByRole('heading', { name: '¿Usas tecnología?' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: '¿Cómo estás?' })).not.toBeInTheDocument();
  });

  it('indica qué elementos faltan en la categoría seleccionada', async () => {
    getCatalogQuestionsForBuilder.mockResolvedValue({ content: [], totalElements: 0 });
    getCatalogBlocksForBuilder.mockResolvedValue({ content: [], totalElements: 0 });
    renderSidebar({ categoryFilter: 'SALUD', categoryDetails: { code: 'SALUD', name: 'Salud', icon: 'BsPeople' } });

    const questionMessage = await screen.findByText(/No hay preguntas disponibles para la categoría/);
    const blockMessage = await screen.findByText(/No hay bloques disponibles para la categoría/);
    for (const message of [questionMessage, blockMessage]) {
      expect(message).toHaveTextContent(/categoría Salud\./);
      expect(message.querySelector('.catalog-empty-category svg')).toBeInTheDocument();
    }
  });

  it('mantiene el mensaje de búsqueda cuando sí hay elementos en la categoría', async () => {
    const user = userEvent.setup();
    getCatalogQuestionsForBuilder.mockResolvedValue({ content: [{ ...question, category: 'SALUD' }], totalElements: 1 });
    renderSidebar({ categoryFilter: 'SALUD', showBlocks: false });

    await screen.findByRole('heading', { name: '¿Cómo estás?' });
    await user.type(screen.getByRole('searchbox', { name: 'Buscar preguntas fijas' }), 'inexistente');
    expect(screen.getByText(/No se han encontrado preguntas/)).toBeVisible();
    expect(screen.queryByText(/No hay preguntas disponibles para la categoría/)).not.toBeInTheDocument();
  });

  it('desplaza la vista hasta cada elemento añadido con el botón', async () => {
    const user = userEvent.setup();
    const scrollIntoView = vi.spyOn(Element.prototype, 'scrollIntoView');

    function BuilderWithSidebar() {
      const [items, setItems] = useState([]);
      const addItem = (item) => setItems((current) => [...current, item]);
      return (
        <MemoryRouter>
          <CatalogSidebar embedded onAddQuestion={addItem} onAddBlock={addItem} />
          <div className="block-questions-list">
            {items.map((item) => <div key={item.id} className="block-question-item">{item.statement || item.name}</div>)}
          </div>
        </MemoryRouter>
      );
    }

    render(<BuilderWithSidebar />);
    await user.click(await screen.findByRole('button', { name: 'Añadir pregunta: ¿Cómo estás?' }));
    await waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(1));
    expect(scrollIntoView.mock.contexts[0]).toHaveTextContent('¿Cómo estás?');

    await user.click(screen.getByRole('button', { name: 'Añadir bloque: Hábitos' }));
    await waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(2));
    expect(scrollIntoView.mock.contexts[1]).toHaveTextContent('Hábitos');
    expect(scrollIntoView).toHaveBeenLastCalledWith({ behavior: 'smooth', block: 'center', inline: 'nearest' });
  });

  it('abre los detalles de un bloque sin salir del formulario ni perder cambios', async () => {
    const user = userEvent.setup();

    function EditorWithSidebar() {
      const [title, setTitle] = useState('');
      const location = useLocation();
      return (
        <>
          <span data-testid="current-path">{location.pathname}</span>
          <input aria-label="Título sin guardar" value={title} onChange={(event) => setTitle(event.target.value)} />
          <CatalogSidebar embedded showQuestions={false} />
        </>
      );
    }

    render(<MemoryRouter initialEntries={['/catalogo/crear/plantilla']}><EditorWithSidebar /></MemoryRouter>);
    await user.type(screen.getByRole('textbox', { name: 'Título sin guardar' }), 'Mi plantilla');
    await user.click(await screen.findByRole('button', { name: 'Ver detalles del bloque: Hábitos' }));

    const dialog = await screen.findByRole('dialog', { name: 'DETALLES DEL BLOQUE' });
    expect(await screen.findByText('Introducción')).toBeVisible();
    expect(dialog).toContainElement(screen.getByRole('heading', { name: 'VER DETALLES:' }));
    expect(dialog.querySelector('.catalog-detail-card')).toBeInTheDocument();
    expect(dialog.querySelector('.catalog-detail-title-bar')).toHaveTextContent('HÁBITOS');
    expect(dialog.querySelector('.catalog-detail-metadata')).toHaveTextContent('Ana');
    expect(dialog.querySelector('.catalog-detail-structure--block')).toHaveTextContent('ESTRUCTURA DEL BLOQUE');
    expect(dialog.querySelector('.catalog-detail-builder-item')).toHaveTextContent('Introducción');
    expect(dialog).toHaveTextContent('Descripción del bloque');
    expect(getCatalogBlockById).toHaveBeenCalledWith(7);
    expect(screen.getByTestId('current-path')).toHaveTextContent('/catalogo/crear/plantilla');
    expect(screen.getByRole('textbox', { name: 'Título sin guardar' })).toHaveValue('Mi plantilla');

    await user.click(screen.getByRole('button', { name: 'Cerrar detalles del bloque' }));
    expect(screen.queryByRole('dialog', { name: 'DETALLES DEL BLOQUE' })).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Título sin guardar' })).toHaveValue('Mi plantilla');
  });

  it('permite reintentar la carga de detalles sin cambiar de página', async () => {
    const user = userEvent.setup();
    getCatalogBlockById.mockRejectedValueOnce(new Error('Bloque no disponible'));
    renderSidebar({ showQuestions: false });

    await user.click(await screen.findByRole('button', { name: 'Ver detalles del bloque: Hábitos' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Bloque no disponible');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Introducción')).toBeVisible();
    expect(getCatalogBlockById).toHaveBeenCalledTimes(2);
  });

  it('cierra el modal con Escape y mantiene abierto el catálogo lateral', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><CatalogSidebar showQuestions={false} defaultOpen /></MemoryRouter>);

    await user.click(await screen.findByRole('button', { name: 'Ver detalles del bloque: Hábitos' }));
    expect(await screen.findByRole('dialog', { name: 'DETALLES DEL BLOQUE' })).toBeVisible();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'DETALLES DEL BLOQUE' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cerrar catálogo de elementos' })).toBeVisible();
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
    const filter = screen.getByRole('button', { name: 'Filtrar preguntas por tipo: Filtrar' });
    expect(filter).toHaveTextContent(/^Filtrar$/);
    await user.click(filter);
    fireEvent.click(screen.getByRole('button', { name: 'Numérica' }));
    expect(filter).toHaveTextContent(/^Numérica$/);
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
