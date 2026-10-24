import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { approveElementRequest, getPendingElementRequests, rejectElementRequest } from '../../../api/elementRequestsApi.js';
import { ElementRequestsPage } from './ElementRequestsPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/elementRequestsApi.js', () => ({
  approveElementRequest: vi.fn(),
  getPendingElementRequests: vi.fn(),
  rejectElementRequest: vi.fn(),
}));
vi.mock('../../../components/Question/Question.jsx', () => ({
  Question: ({ data }) => <p>Vista previa: {data.statement}</p>,
}));

const request = {
  id: 5,
  elementType: 'FIXED_QUESTION',
  requestPayload: { question: { statement: '¿Qué opinas?', questionType: 'TEXT' } },
  requestedByName: 'Ana García',
  requestedByRole: 'EDITOR',
  requestedByEmail: 'ana@example.com',
  requestedByCountry: 'ES',
  message: 'Pregunta necesaria',
  createdAt: '2026-10-01T12:00:00Z',
};

describe('ElementRequestsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getPendingElementRequests.mockResolvedValue([]);
  });

  it('muestra el estado vacío sin solicitudes pendientes', async () => {
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByRole('heading', { name: 'No hay solicitudes pendientes' })).toBeVisible();
  });

  it('filtra las solicitudes y confirma la aprobación', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([request]);
    approveElementRequest.mockResolvedValue(null);
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Pregunta necesaria')).toBeVisible();
    expect(screen.queryByText('Vista previa: ¿Qué opinas?')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ver solicitud' }));
    expect(await screen.findByText('Vista previa: ¿Qué opinas?')).toBeVisible();
    await user.type(screen.getByRole('searchbox', { name: /Introduce una palabra/ }), 'sin coincidencias');
    expect(screen.getByRole('heading', { name: 'No hay coincidencias' })).toBeVisible();
    await user.clear(screen.getByRole('searchbox', { name: /Introduce una palabra/ }));
    await user.click(screen.getByRole('button', { name: 'Aceptar' }));
    const dialog = screen.getByRole('dialog', { name: 'Aceptar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Aceptar' }));
    expect(approveElementRequest).toHaveBeenCalledWith(5);
    expect(await screen.findByRole('heading', { name: 'No hay solicitudes pendientes' })).toBeVisible();
  });

  it('combina la búsqueda con el tipo sin borrar el texto al restablecer el filtro', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([
      request,
      { ...request, id: 6, elementType: 'BLOCK', requestPayload: { name: 'Hábitos de clase', items: [] } },
      { ...request, id: 7, elementType: 'TEMPLATE', requestedByName: 'Luis', requestedByEmail: 'luis@example.com', requestPayload: { title: 'Modelo familiar', items: [] } },
    ]);
    renderWithRouter(<ElementRequestsPage />);

    const search = screen.getByRole('searchbox', { name: /Introduce una palabra/ });
    expect(await screen.findByRole('heading', { name: 'Modelo familiar', level: 2 })).toBeVisible();
    await user.type(search, 'Ana');
    expect(screen.getByRole('heading', { name: '¿Qué opinas?', level: 2 })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos de clase', level: 2 })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Modelo familiar', level: 2 })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    const filter = screen.getByRole('group', { name: 'Filtrar' });
    await user.click(within(filter).getByRole('checkbox', { name: 'Preguntas' }));
    await user.click(within(filter).getByRole('checkbox', { name: 'Plantillas' }));
    await user.click(within(filter).getByRole('button', { name: 'Aplicar' }));
    expect(screen.queryByRole('heading', { name: '¿Qué opinas?', level: 2 })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Hábitos de clase', level: 2 })).toBeVisible();

    await user.clear(search);
    expect(screen.getByRole('heading', { name: 'Hábitos de clase', level: 2 })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Modelo familiar', level: 2 })).not.toBeInTheDocument();

    await user.type(search, 'Ana');
    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(screen.getByRole('checkbox', { name: 'Preguntas' }));
    await user.click(screen.getByRole('button', { name: 'Aplicar' }));
    expect(screen.getByRole('heading', { name: '¿Qué opinas?', level: 2 })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos de clase', level: 2 })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Modelo familiar', level: 2 })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Filtrar:/ }));
    await user.click(screen.getByRole('button', { name: 'Borrar filtros' }));
    expect(search).toHaveValue('Ana');
    expect(screen.getByRole('heading', { name: '¿Qué opinas?', level: 2 })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos de clase', level: 2 })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Modelo familiar', level: 2 })).not.toBeInTheDocument();
  });

  it('mantiene visible la propuesta si falla el rechazo', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([request]);
    rejectElementRequest.mockRejectedValue(new Error('Sin red'));
    renderWithRouter(<ElementRequestsPage />);
    await user.click(await screen.findByRole('button', { name: 'Ver solicitud' }));
    await user.click(screen.getByRole('button', { name: 'Denegar' }));
    const dialog = screen.getByRole('dialog', { name: 'Denegar solicitud' });
    await user.click(within(dialog).getByRole('button', { name: 'Denegar' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se ha podido resolver la solicitud.');
    expect(screen.getByText('¿Qué opinas?')).toBeVisible();
  });

  it('muestra y contrae la estructura de un bloque solicitado', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([{ ...request, id: 6, elementType: 'BLOCK', requestPayload: {
      name: 'Hábitos', items: [{ itemType: 'TEXT', textBody: 'Introducción' }, { itemType: 'QUESTION', fixedQuestionId: 9 }],
    } }]);
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Pregunta necesaria')).toBeVisible();
    expect(screen.queryByText('Introducción')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ver solicitud' }));
    expect(screen.getByText('Introducción')).toBeVisible();
    expect(screen.getByText('Vista previa: Pregunta del catálogo #9')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ocultar solicitud' }));
    expect(screen.queryByText('Introducción')).not.toBeInTheDocument();
    expect(screen.getByText('Pregunta necesaria')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver solicitud' }));
    expect(screen.getByText('Introducción')).toBeVisible();
  });

  it('muestra referencias de la plantilla y el error de carga', async () => {
    const user = userEvent.setup();
    getPendingElementRequests.mockResolvedValue([{ ...request, id: 7, elementType: 'TEMPLATE', requestPayload: {
      title: 'Modelo', items: [{ itemType: 'TEXT', textBody: 'Contexto' }, { itemType: 'BLOCK', blockId: 8 }],
    } }]);
    const view = renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByText('Pregunta necesaria')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver solicitud' }));
    expect(screen.getByText('Contexto')).toBeVisible();
    expect(screen.getByText('A. Bloque del catálogo #8')).toBeVisible();
    view.unmount();
    getPendingElementRequests.mockRejectedValue(new Error('Red'));
    renderWithRouter(<ElementRequestsPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron obtener las solicitudes del catálogo.');
  });
});
