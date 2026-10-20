import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { replaceSurveyContent } from '../../../api/surveyContentApi.js';
import {
  acceptCollaborationRequest, acceptPublicationRequest, createCollaborationRequest, getCollaborationRequests,
  getCollaborators, getPublicationRequests, rejectCollaborationRequest, rejectPublicationRequest,
  removeCollaborator, requestPublication, updateCollaboratorPermission,
} from '../../../api/collaborationsApi.js';
import {
  getSurveyById,
  updateAdminSurveyLifecycle,
  updateAdminSurveyVisibility,
  updateSurvey,
  updateSurveyLifecycle,
} from '../../../api/surveysApi.js';
import { EditSurveyPage } from './EditSurveyPage.jsx';
import { renderWithDataRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  getSurveyById: vi.fn(),
  updateAdminSurveyLifecycle: vi.fn(),
  updateAdminSurveyVisibility: vi.fn(),
  updateSurvey: vi.fn(),
  updateSurveyLifecycle: vi.fn(),
}));
vi.mock('../../../api/surveyContentApi.js', () => ({ replaceSurveyContent: vi.fn() }));
vi.mock('../../../api/collaborationsApi.js', () => ({
  acceptCollaborationRequest: vi.fn(), acceptPublicationRequest: vi.fn(), createCollaborationRequest: vi.fn(),
  getCollaborationRequests: vi.fn(() => Promise.resolve([])), getCollaborators: vi.fn(() => Promise.resolve([])),
  getPublicationRequests: vi.fn(() => Promise.resolve([])), rejectCollaborationRequest: vi.fn(),
  rejectPublicationRequest: vi.fn(), removeCollaborator: vi.fn(), requestPublication: vi.fn(),
  updateCollaboratorPermission: vi.fn(),
}));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([])) }));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));
vi.mock('../../../components/SurveyBuilder/SurveyBuilder.jsx', () => ({
  SurveyBuilder: ({ data, onChange, readOnly }) => (
    <div>
      <p>Estructura con {data.items.length} elementos</p>
      {!readOnly && <button type="button" onClick={() => onChange({ items: [{ itemType: 'TEXT', textBody: 'Introducción' }] })}>Añadir introducción</button>}
    </div>
  ),
}));
vi.mock('../../../components/SurveyPreviewModal/SurveyPreviewModal.jsx', () => ({ SurveyPreviewModal: () => null }));

const survey = {
  id: 12,
  ownerId: 7,
  title: 'Hábitos digitales',
  description: 'Estudio sobre tecnología',
  lifecycleStatus: 'DRAFT',
  accessMode: 'PUBLIC',
  visibility: 'UNLISTED',
  fontFamily: 'TIMES_NEW_ROMAN',
  tags: [],
  contentItems: [],
};

function renderPage(route = '/encuestas/12/editar', user = { id: 7, role: 'EDITOR' }) {
  return renderWithDataRouter(
    [{ path: '/encuestas/:surveyId/editar', element: <EditSurveyPage /> }],
    { route, auth: { user } },
  );
}

describe('EditSurveyPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getSurveyById.mockResolvedValue(survey);
    updateSurvey.mockResolvedValue(survey);
    replaceSurveyContent.mockResolvedValue(null);
    getCollaborators.mockResolvedValue([]);
    getCollaborationRequests.mockResolvedValue([]);
    getPublicationRequests.mockResolvedValue([]);
  });

  it('muestra la vista de detalles para supervisión administrativa', async () => {
    renderPage('/encuestas/12/editar?vista=detalles', { id: 1, role: 'ADMIN' });
    expect(await screen.findByRole('heading', { name: 'VER ENCUESTA' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    expect(screen.getByText('Estructura con 0 elementos')).toBeVisible();
  });

  it('guarda la estructura y las etiquetas de una encuesta propia', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Añadir introducción' }));
    await user.type(screen.getByRole('textbox', { name: 'Nueva etiqueta' }), '#Vida Digital');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Cambios guardados correctamente.');
    expect(replaceSurveyContent).toHaveBeenCalledWith('12', {
      items: [{ itemType: 'TEXT', position: 1, textBody: 'Introducción' }],
    });
    expect(updateSurvey).toHaveBeenCalledWith('12', expect.objectContaining({ tags: ['vida-digital'] }));
  });

  it('guarda un cambio de título desde configuración', async () => {
    const user = userEvent.setup();
    updateSurvey.mockResolvedValue({ ...survey, title: 'Nuevo título' });
    renderPage('/encuestas/12/editar?seccion=configuracion');
    const title = await screen.findByRole('textbox', { name: /Título de la encuesta/ });
    await user.clear(title);
    await user.type(title, 'Nuevo título');
    await user.click(screen.getByRole('button', { name: 'Actualizar configuración' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Configuración guardada correctamente.');
    expect(updateSurvey).toHaveBeenCalledWith('12', expect.objectContaining({ title: 'Nuevo título' }));
  });

  it('permite al administrador cerrar y ocultar una encuesta abierta', async () => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, lifecycleStatus: 'OPEN', visibility: 'LISTED' });
    updateAdminSurveyLifecycle.mockResolvedValue({ ...survey, lifecycleStatus: 'CLOSED', visibility: 'LISTED' });
    updateAdminSurveyVisibility.mockResolvedValue({ ...survey, lifecycleStatus: 'CLOSED', visibility: 'UNLISTED' });
    renderPage('/encuestas/12/editar?seccion=configuracion', { id: 1, role: 'ADMIN' });
    await user.click(await screen.findByRole('button', { name: 'Cerrar encuesta' }));
    let dialog = screen.getByRole('dialog', { name: 'Cerrar encuesta' });
    await user.click(within(dialog).getByRole('button', { name: 'Cerrar encuesta' }));
    expect(await screen.findByRole('status')).toHaveTextContent('La encuesta ahora está cerrada.');
    expect(updateAdminSurveyLifecycle).toHaveBeenCalledWith('12', 'CLOSED');
    await user.click(screen.getByRole('button', { name: 'Ocultar encuesta' }));
    dialog = screen.getByRole('dialog', { name: 'Ocultar encuesta' });
    await user.click(within(dialog).getByRole('button', { name: 'Ocultar encuesta' }));
    expect(await screen.findByRole('status')).toHaveTextContent('La encuesta se ha ocultado');
    expect(updateAdminSurveyVisibility).toHaveBeenCalledWith('12', 'UNLISTED');
  });

  it('envía invitaciones y gestiona colaboraciones como propietario', async () => {
    const user = userEvent.setup();
    getCollaborators.mockResolvedValue([{ userId: 9, name: 'Eva', email: 'eva@example.com', permission: 'EDIT' }]);
    getCollaborationRequests.mockResolvedValue([{ id: 5, userId: 10, requestType: 'ACCESS_REQUEST', permission: 'VIEW' }]);
    getPublicationRequests.mockResolvedValue([]);
    createCollaborationRequest.mockResolvedValue({ id: 6 });
    acceptCollaborationRequest.mockResolvedValue({});
    removeCollaborator.mockResolvedValue({});
    renderPage('/encuestas/12/editar?seccion=colaboraciones');
    expect(await screen.findByRole('heading', { name: 'INVITAR A UN COLABORADOR' })).toBeVisible();
    await user.type(screen.getByRole('textbox', { name: /Correo electrónico/ }), '  EVA@EXAMPLE.COM  ');
    await user.click(screen.getByRole('button', { name: 'Enviar invitación' }));
    expect(createCollaborationRequest).toHaveBeenCalledWith('12', expect.objectContaining({ email: 'eva@example.com', permission: 'EDIT' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Invitación interna enviada.');
    await user.click(screen.getByRole('button', { name: 'Aceptar solicitud' }));
    expect(acceptCollaborationRequest).toHaveBeenCalledWith('12', 5);
    await user.click(screen.getByRole('button', { name: 'Revocar la colaboración de Eva' }));
    expect(removeCollaborator).toHaveBeenCalledWith('12', 9);
  });

  it('permite al colaborador solicitar la publicación de la encuesta', async () => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, ownerId: 99, canEdit: true, canPreview: true });
    requestPublication.mockResolvedValue({ id: 8 });
    renderPage('/encuestas/12/editar?seccion=colaboraciones');
    expect(await screen.findByRole('heading', { name: 'SOLICITAR PUBLICACIÓN' })).toBeVisible();
    await user.type(screen.getByRole('textbox', { name: 'Mensaje opcional' }), 'Revisada');
    await user.click(screen.getByRole('button', { name: 'Solicitar publicación' }));
    expect(requestPublication).toHaveBeenCalledWith('12', 'Revisada');
    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud de publicación enviada.');
  });

  it('impide publicar un borrador sin contenido válido', async () => {
    const user = userEvent.setup();
    renderPage('/encuestas/12/editar?seccion=configuracion');
    await user.click(await screen.findByRole('button', { name: 'Publicar encuesta' }));
    expect(screen.getByRole('alert')).toBeVisible();
    expect(updateSurveyLifecycle).not.toHaveBeenCalled();
  });

  it('muestra un error si falla el guardado de la configuración', async () => {
    const user = userEvent.setup();
    updateSurvey.mockRejectedValue(new Error('Servicio no disponible'));
    renderPage('/encuestas/12/editar?seccion=configuracion');
    await user.click(await screen.findByRole('button', { name: 'Actualizar configuración' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
  });

  it('muestra el fallo de carga sin presentar una encuesta inexistente', async () => {
    getSurveyById.mockRejectedValue(new Error('Encuesta no encontrada'));
    renderPage('/encuestas/12/editar?vista=detalles', { id: 1, role: 'ADMIN' });
    expect(await screen.findByRole('alert')).toHaveTextContent('Encuesta no encontrada');
    expect(screen.queryByRole('heading', { name: 'Hábitos digitales' })).not.toBeInTheDocument();
  });

  it('valida el título de configuración antes de enviarlo', async () => {
    const user = userEvent.setup();
    renderPage('/encuestas/12/editar?seccion=configuracion');
    const title = await screen.findByRole('textbox', { name: /Título de la encuesta/ });
    await user.clear(title);
    await user.click(screen.getByRole('button', { name: 'Actualizar configuración' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Introduce un título');
    expect(updateSurvey).not.toHaveBeenCalled();
  });

  it('permite cerrar la encuesta propia tras confirmar', async () => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, lifecycleStatus: 'OPEN' });
    updateSurveyLifecycle.mockResolvedValue({ ...survey, lifecycleStatus: 'CLOSED' });
    renderPage('/encuestas/12/editar?seccion=configuracion');
    await user.click(await screen.findByRole('button', { name: 'Cerrar encuesta' }));
    await user.click(within(screen.getByRole('dialog', { name: 'Cerrar encuesta' })).getByRole('button', { name: 'Cerrar encuesta' }));
    expect(await screen.findByRole('status')).toHaveTextContent('La encuesta ahora está cerrada.');
    expect(updateSurveyLifecycle).toHaveBeenCalledWith('12', 'CLOSED');
  });

  it('gestiona solicitudes y permisos de colaboradores', async () => {
    const user = userEvent.setup();
    getCollaborators.mockResolvedValue([{ userId: 9, name: 'Eva', email: 'eva@example.com', permission: 'EDIT' }]);
    getCollaborationRequests.mockResolvedValue([{ id: 5, userId: 10, requestType: 'ACCESS_REQUEST', permission: 'VIEW' }]);
    getPublicationRequests.mockResolvedValue([{ id: 6, requestedByUserId: 9, requestedByName: 'Eva' }]);
    updateCollaboratorPermission.mockResolvedValue({});
    rejectCollaborationRequest.mockResolvedValue({});
    acceptPublicationRequest.mockResolvedValue({});
    rejectPublicationRequest.mockResolvedValue({});
    renderPage('/encuestas/12/editar?seccion=colaboraciones');
    await user.click(await screen.findByRole('button', { name: /Permiso de Eva:/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Solo lectura' }));
    await waitFor(() => expect(updateCollaboratorPermission).toHaveBeenCalledWith('12', 9, 'VIEW'));
    await user.click(screen.getByRole('button', { name: 'Rechazar solicitud' }));
    await waitFor(() => expect(rejectCollaborationRequest).toHaveBeenCalledWith('12', 5));
    await user.click(screen.getByRole('button', { name: 'Aceptar publicación' }));
    await waitFor(() => expect(acceptPublicationRequest).toHaveBeenCalledWith('12', 6));
    await user.click(screen.getByRole('button', { name: 'Rechazar publicación' }));
    await waitFor(() => expect(rejectPublicationRequest).toHaveBeenCalledWith('12', 6));
  });
});
