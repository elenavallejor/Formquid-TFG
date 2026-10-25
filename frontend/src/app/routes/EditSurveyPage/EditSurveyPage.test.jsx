import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToStaticMarkup } from 'react-dom/server';
import { BsPeople } from 'react-icons/bs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { replaceSurveyContent } from '../../../api/surveyContentApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import {
  acceptCollaborationRequest, acceptPublicationRequest, createCollaborationRequest, getCollaborationRequests,
  getCollaborators, getPublicationRequests, rejectCollaborationRequest, rejectPublicationRequest,
  removeCollaborator, requestPublication, updateCollaboratorPermission,
} from '../../../api/collaborationsApi.js';
import {
  deleteSurvey,
  getSurveyById,
  updateAdminSurveyLifecycle,
  updateAdminSurveyVisibility,
  updateSurvey,
  updateSurveyLifecycle,
  uploadSurveyCover,
} from '../../../api/surveysApi.js';
import { EditSurveyPage } from './EditSurveyPage.jsx';
import { renderWithDataRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  deleteSurvey: vi.fn(),
  getSurveyById: vi.fn(),
  updateAdminSurveyLifecycle: vi.fn(),
  updateAdminSurveyVisibility: vi.fn(),
  updateSurvey: vi.fn(),
  updateSurveyLifecycle: vi.fn(),
  uploadSurveyCover: vi.fn(),
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
  afterEach(() => vi.unstubAllGlobals());
  beforeEach(() => {
    vi.resetAllMocks();
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:cover-preview') });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
    getSurveyById.mockResolvedValue(survey);
    deleteSurvey.mockResolvedValue(undefined);
    updateSurvey.mockResolvedValue(survey);
    replaceSurveyContent.mockResolvedValue(null);
    getCollaborators.mockResolvedValue([]);
    getCollaborationRequests.mockResolvedValue([]);
    getPublicationRequests.mockResolvedValue([]);
  });

  it('sube y guarda una portada local desde configuración', async () => {
    const user = userEvent.setup();
    const file = new File(['imagen'], 'portada.jpg', { type: 'image/jpeg' });
    uploadSurveyCover.mockResolvedValue({ ...survey, imageKey: '/api/v1/public/surveys/12/cover/imagen.jpg' });
    renderPage('/encuestas/12/editar?seccion=configuracion');

    await user.upload(await screen.findByLabelText('Imagen del dispositivo'), file);
    await user.click(screen.getByRole('button', { name: 'Actualizar configuración' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Configuración guardada correctamente.');
    expect(uploadSurveyCover).toHaveBeenCalledWith('12', file);
  });

  it('permite elegir una portada de la galería al editar y espera a que cargue antes de guardar', async () => {
    const user = userEvent.setup();
    let resolveImage;
    vi.stubGlobal('fetch', vi.fn(() => new Promise((resolve) => { resolveImage = resolve; })));
    uploadSurveyCover.mockResolvedValue({ ...survey, imageKey: '/api/v1/public/surveys/12/cover/aula.png' });
    renderPage('/encuestas/12/editar?seccion=configuracion');

    await user.click(await screen.findByRole('button', { name: 'ABRIR GALERÍA DE FOTOS' }));
    await user.click(screen.getByRole('button', { name: 'Usar imagen: Aula' }));
    expect(screen.getByRole('button', { name: 'Actualizar configuración' })).toBeDisabled();

    resolveImage({ ok: true, blob: () => Promise.resolve(new Blob(['imagen'], { type: 'image/png' })) });
    expect(await screen.findByText('aula.png')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Actualizar configuración' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Configuración guardada correctamente.');
    expect(uploadSurveyCover).toHaveBeenCalledWith('12', expect.objectContaining({ name: 'aula.png', type: 'image/png' }));
  });

  it('muestra los avisos como subtítulo bajo Acceso y visibilidad', async () => {
    renderPage('/encuestas/12/editar?seccion=configuracion');
    const heading = await screen.findByRole('heading', { name: 'ACCESO Y VISIBILIDAD' });
    const subtitles = heading.nextElementSibling;

    expect(subtitles).toHaveClass('edit-survey-access-notes');
    expect(subtitles.querySelectorAll('p')).toHaveLength(2);
    expect(subtitles.querySelectorAll('p')[0]).toHaveTextContent('Los borradores permanecen no listados.');
    expect(subtitles.querySelectorAll('p')[1]).toHaveTextContent('Una encuesta oculta no es privada.');
    expect(heading.closest('section').querySelector('.create-survey-security-note')).not.toBeInTheDocument();
  });

  it('muestra la vista de detalles para supervisión administrativa', async () => {
    renderPage('/encuestas/12/editar?vista=detalles', { id: 1, role: 'ADMIN' });
    expect(await screen.findByRole('heading', { name: 'VER ENCUESTA' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hábitos digitales' })).toBeVisible();
    expect(screen.getByText('Estructura con 0 elementos')).toBeVisible();
    expect(screen.getByText('SIN PUBLICAR')).toHaveClass('table-badge-draft');
    expect(screen.queryByText('PÚBLICO')).not.toBeInTheDocument();
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

  it('muestra el nombre de la categoría en la información general', async () => {
    getSurveyById.mockResolvedValue({ ...survey, category: 'REDES_SOCIALES' });
    getCategories.mockResolvedValue([{ code: 'REDES_SOCIALES', name: 'Redes sociales', icon: 'BsPeople' }]);
    renderPage();

    const category = await screen.findByRole('textbox', { name: 'Categoría' });
    await waitFor(() => expect(category).toHaveValue('Redes sociales'));
    expect(category).toHaveAttribute('readonly');
    expect(category.parentElement.querySelector('svg')?.outerHTML)
      .toBe(renderToStaticMarkup(<BsPeople aria-hidden="true" />));
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

  it('confirma el borrado de una encuesta cerrada y vuelve al listado aunque haya cambios sin guardar', async () => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, lifecycleStatus: 'CLOSED' });
    renderWithDataRouter([
      { path: '/encuestas/:surveyId/editar', element: <EditSurveyPage /> },
      { path: '/encuestas', element: <h1>Listado de encuestas</h1> },
    ], { route: '/encuestas/12/editar?seccion=configuracion', auth: { user: { id: 7, role: 'EDITOR' } } });

    const deleteButton = await screen.findByRole('button', { name: 'Eliminar encuesta' });
    const lifecycleCard = screen.getByRole('heading', { name: 'CICLO DE VIDA' }).closest('section');
    const deleteCard = screen.getByRole('heading', { name: 'ELIMINAR ENCUESTA' }).closest('section');
    expect(lifecycleCard.compareDocumentPosition(deleteCard) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    await user.click(deleteButton);
    let dialog = screen.getByRole('dialog', { name: 'Eliminar encuesta' });
    expect(dialog).toHaveTextContent('Esta acción no se puede deshacer.');
    expect(deleteSurvey).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(deleteSurvey).not.toHaveBeenCalled();

    await user.type(screen.getByRole('textbox', { name: /Título de la encuesta/ }), ' editada');
    await user.click(deleteButton);
    dialog = screen.getByRole('dialog', { name: 'Eliminar encuesta' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar encuesta' }));
    expect(deleteSurvey).toHaveBeenCalledWith('12');
    expect(await screen.findByRole('heading', { name: 'Listado de encuestas' })).toBeVisible();
    expect(screen.queryByRole('dialog', { name: 'Salir sin guardar' })).not.toBeInTheDocument();
  });

  it.each(['DRAFT', 'OPEN'])('permite al propietario solicitar el borrado en estado %s', async (lifecycleStatus) => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, lifecycleStatus });
    renderPage('/encuestas/12/editar?seccion=configuracion');
    const button = await screen.findByRole('button', { name: 'Eliminar encuesta' });
    expect(button).toBeEnabled();
    await user.click(button);
    await user.click(within(screen.getByRole('dialog', { name: 'Eliminar encuesta' })).getByRole('button', { name: 'Eliminar encuesta' }));
    expect(deleteSurvey).toHaveBeenCalledWith('12');
  });

  it('no ofrece el borrado a colaboradores', async () => {
    getSurveyById.mockResolvedValue({ ...survey, ownerId: 99, canEdit: true });
    renderPage('/encuestas/12/editar?seccion=configuracion', { id: 7, role: 'EDITOR' });
    await screen.findByRole('textbox', { name: /Título de la encuesta/ });
    expect(screen.queryByRole('heading', { name: 'ELIMINAR ENCUESTA' })).not.toBeInTheDocument();
  });

  it('mantiene abierto el modal si falla la eliminación', async () => {
    const user = userEvent.setup();
    getSurveyById.mockResolvedValue({ ...survey, lifecycleStatus: 'CLOSED' });
    deleteSurvey.mockRejectedValue(new Error('No se pudo eliminar'));
    renderPage('/encuestas/12/editar?seccion=configuracion');

    await user.click(await screen.findByRole('button', { name: 'Eliminar encuesta' }));
    const dialog = screen.getByRole('dialog', { name: 'Eliminar encuesta' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar encuesta' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se pudo eliminar');
    expect(dialog).toBeVisible();
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
