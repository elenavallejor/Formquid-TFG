import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  deleteCatalogTemplate,
  getCatalogBlockById,
  getCatalogQuestionById,
  getCatalogTemplateById,
} from '../../../api/catalogApi.js';
import { ElementDetailPage } from './ElementDetailPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/catalogApi.js', () => ({
  deleteCatalogBlock: vi.fn(), deleteCatalogQuestion: vi.fn(), deleteCatalogTemplate: vi.fn(),
  getCatalogBlockById: vi.fn(), getCatalogQuestionById: vi.fn(), getCatalogTemplateById: vi.fn(),
}));
vi.mock('../../../components/Question/Question.jsx', () => ({ Question: ({ data }) => <p>Vista de pregunta: {data.statement}</p> }));
vi.mock('../../../components/Block/Block.jsx', () => ({ Block: ({ data }) => <p>Vista de bloque: {data.name}</p> }));

function renderPage(type, role = 'EDITOR') {
  renderWithRouter(
    <Routes>
      <Route path="/catalogo/detalles/:elementType/:elementId" element={<ElementDetailPage />} />
      <Route path="/catalogo/plantillas" element={<h1>Listado de plantillas</h1>} />
    </Routes>,
    { route: `/catalogo/detalles/${type}/12`, auth: { user: { id: 7, role } } },
  );
}

describe('ElementDetailPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCatalogQuestionById.mockResolvedValue({
      id: 12, creatorId: 7, statement: '¿Qué opinas?', questionType: 'TEXT', settings: { maxLength: 300 },
    });
    getCatalogBlockById.mockResolvedValue({
      id: 12, creatorId: 7, name: 'Bloque inicial', contentItems: [{ itemType: 'TEXT', textBody: 'Introducción' }],
    });
    getCatalogTemplateById.mockResolvedValue({ id: 12, creatorId: 7, title: 'Plantilla inicial', contentItems: [] });
  });

  it('muestra el tipo y longitud de una pregunta fija', async () => {
    renderPage('pregunta');
    expect(await screen.findByRole('heading', { name: '¿Qué opinas?' })).toBeVisible();
    expect(screen.getByText('Vista de pregunta: ¿Qué opinas?')).toBeVisible();
    expect(screen.getByText('Longitud máxima')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Editar pregunta' })).toBeVisible();
  });

  it('presenta el contenido de un bloque', async () => {
    renderPage('bloque', 'EDITOR');
    expect(await screen.findByRole('heading', { name: 'BLOQUE INICIAL' })).toBeVisible();
    expect(screen.getByText('Introducción')).toBeVisible();
  });

  it('muestra los ajustes de escala de una pregunta y oculta las acciones a otros usuarios', async () => {
    getCatalogQuestionById.mockResolvedValue({
      id: 12, creatorId: 8, statement: 'Valora el servicio', questionType: 'SCALE',
      settings: { minValue: 1, maxValue: 5, step: 1, leftLabel: 'Poco', rightLabel: 'Mucho', scaleDisplayType: 'STARS' },
    });
    renderPage('pregunta');
    expect(await screen.findByRole('heading', { name: 'Valora el servicio' })).toBeVisible();
    expect(screen.getByText('Valor mínimo')).toBeVisible();
    expect(screen.getByText('Valor máximo')).toBeVisible();
    expect(screen.getByText('Paso')).toBeVisible();
    expect(screen.getByText('Etiqueta inicial')).toBeVisible();
    expect(screen.getByText('Poco')).toBeVisible();
    expect(screen.getByText('Etiqueta final')).toBeVisible();
    expect(screen.getByText('Mucho')).toBeVisible();
    expect(screen.getByText('Estrellas')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Eliminar pregunta' })).not.toBeInTheDocument();
  });

  it('muestra los límites de selección múltiple', async () => {
    getCatalogQuestionById.mockResolvedValue({
      id: 12, creatorId: 7, statement: 'Elige opciones', questionType: 'MULTIPLE_CHOICE',
      settings: { minSelections: 1, maxSelections: 3 },
    });
    renderPage('pregunta');
    expect(await screen.findByRole('heading', { name: 'Elige opciones' })).toBeVisible();
    expect(screen.getByText('Selecciones mínimas')).toBeVisible();
    expect(screen.getByText('Selecciones máximas')).toBeVisible();
    expect(screen.getByText('3')).toBeVisible();
  });

  it('muestra las etiquetas de una pregunta de sí o no', async () => {
    getCatalogQuestionById.mockResolvedValue({
      id: 12, creatorId: 7, statement: '¿Aceptas?', questionType: 'BOOLEAN',
      settings: { trueLabel: 'Acepto', falseLabel: 'Rechazo' },
    });
    renderPage('pregunta');
    expect(await screen.findByRole('heading', { name: '¿Aceptas?' })).toBeVisible();
    expect(screen.getByText('Respuesta afirmativa')).toBeVisible();
    expect(screen.getByText('Acepto')).toBeVisible();
    expect(screen.getByText('Respuesta negativa')).toBeVisible();
    expect(screen.getByText('Rechazo')).toBeVisible();
  });

  it('presenta las preguntas de un bloque junto a su texto introductorio', async () => {
    getCatalogBlockById.mockResolvedValue({
      id: 12, creatorId: 7, name: 'Bloque mixto',
      contentItems: [
        { id: 1, itemType: 'TEXT', textBody: 'Introducción' },
        { id: 2, itemType: 'QUESTION', question: { id: 3, questionType: 'TEXT', statement: 'Pregunta fija' } },
      ],
    });
    renderPage('bloque');
    expect(await screen.findByRole('heading', { name: 'BLOQUE MIXTO' })).toBeVisible();
    expect(screen.getByText('Introducción')).toBeVisible();
    expect(screen.getByText('Vista de pregunta: Pregunta fija')).toBeVisible();
  });

  it('presenta textos, preguntas y bloques en una plantilla', async () => {
    getCatalogTemplateById.mockResolvedValue({
      id: 12, creatorId: 7, title: 'Plantilla completa',
      contentItems: [
        { id: 1, itemType: 'TEXT', textBody: 'Lee las instrucciones' },
        { id: 2, itemType: 'QUESTION', question: { id: 3, questionType: 'TEXT', statement: 'Primera pregunta' } },
        { id: 4, itemType: 'BLOCK', block: { id: 5, name: 'Bloque A', contentItems: [] } },
      ],
    });
    renderPage('plantilla');
    expect(await screen.findByRole('heading', { name: 'Plantilla completa' })).toBeVisible();
    expect(screen.getByText('Lee las instrucciones')).toBeVisible();
    expect(screen.getByText('Vista de pregunta: Primera pregunta')).toBeVisible();
    expect(screen.getByText('Vista de bloque: Bloque A')).toBeVisible();
  });

  it('permite corregir un fallo de borrado sin salir de la plantilla', async () => {
    const user = userEvent.setup();
    deleteCatalogTemplate.mockRejectedValue(new Error('No autorizado'));
    renderPage('plantilla');
    await user.click(await screen.findByRole('button', { name: 'Eliminar plantilla' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar plantilla' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No autorizado');
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Plantilla inicial' })).toBeVisible();
  });

  it('elimina una plantilla propia después de confirmarlo', async () => {
    const user = userEvent.setup();
    deleteCatalogTemplate.mockResolvedValue(null);
    renderPage('plantilla');
    await user.click(await screen.findByRole('button', { name: 'Eliminar plantilla' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar plantilla' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));
    expect(deleteCatalogTemplate).toHaveBeenCalledWith('12');
    expect(await screen.findByRole('heading', { name: 'Listado de plantillas' })).toBeVisible();
  });

  it('muestra un enlace de regreso si falla la carga', async () => {
    getCatalogTemplateById.mockRejectedValue(new Error('Plantilla no disponible'));
    renderPage('plantilla');
    expect(await screen.findByRole('alert')).toHaveTextContent('Plantilla no disponible');
    expect(screen.getByRole('link', { name: 'Volver al catálogo' })).toHaveAttribute('href', '/catalogo/plantillas');
  });
});
