import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createElementRequest } from '../../../api/elementRequestsApi.js';
import { SuggestElementPage } from './SuggestElementPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/elementRequestsApi.js', () => ({ createElementRequest: vi.fn() }));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));
vi.mock('../../../components/QuestionBuilder/QuestionBuilder.jsx', () => ({
  QuestionBuilder: ({ data, onChange }) => (
    <label>Enunciado
      <input value={data.statement} onChange={(event) => onChange({ ...data, statement: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../components/BlockBuilder/BlockBuilder.jsx', () => ({
  BlockBuilder: ({ data, onChange }) => (
    <label>Nombre del bloque
      <input value={data.name} onChange={(event) => onChange({ ...data, name: event.target.value })} />
    </label>
  ),
}));
vi.mock('../../../components/TemplateBuilder/TemplateBuilder.jsx', () => ({
  TemplateBuilder: ({ data, onChange }) => (
    <label>Título de plantilla
      <input value={data.title} onChange={(event) => onChange({ ...data, title: event.target.value })} />
    </label>
  ),
}));

describe('SuggestElementPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('requiere un motivo y un enunciado antes de enviar una pregunta', async () => {
    const user = userEvent.setup();
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Pregunta fija.*Propón una nueva pregunta/ }));
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Explica brevemente el motivo');
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), 'Es útil');
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debes redactar el enunciado');
    expect(createElementRequest).not.toHaveBeenCalled();
  });

  it('envía una propuesta válida de pregunta fija', async () => {
    const user = userEvent.setup();
    createElementRequest.mockResolvedValue({ id: 4 });
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Pregunta fija.*Propón una nueva pregunta/ }));
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), '  Nueva temática  ');
    await user.type(screen.getByRole('textbox', { name: 'Enunciado' }), '¿Qué opinas?');
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud enviada con éxito.');
    expect(createElementRequest).toHaveBeenCalledWith(expect.objectContaining({
      elementType: 'FIXED_QUESTION',
      message: 'Nueva temática',
      requestPayload: expect.objectContaining({ question: expect.objectContaining({ statement: '¿Qué opinas?' }) }),
    }));
  });

  it('permite cambiar de tipo y muestra fallos del servidor', async () => {
    const user = userEvent.setup();
    createElementRequest.mockRejectedValue(new Error('Servicio no disponible'));
    renderWithRouter(<SuggestElementPage />);
    await user.click(screen.getByRole('button', { name: /Bloque.*Agrupa preguntas/ }));
    expect(screen.getByRole('textbox', { name: 'Nombre del bloque' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: /Plantilla.*Combina preguntas/ }));
    await user.type(screen.getByRole('textbox', { name: /Mensaje para el equipo administrador/ }), 'Hace falta');
    await user.type(screen.getByRole('textbox', { name: 'Título de plantilla' }), 'Plantilla general');
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(createElementRequest).toHaveBeenCalledWith(expect.objectContaining({ elementType: 'TEMPLATE' }));
  });
});
