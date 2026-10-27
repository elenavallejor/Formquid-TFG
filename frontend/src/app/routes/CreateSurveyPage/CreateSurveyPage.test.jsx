import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createSurvey, createSurveyFromImport, createSurveyFromTemplate, uploadSurveyCover } from '../../../api/surveysApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import { CreateSurveyPage } from './CreateSurveyPage.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  createSurvey: vi.fn(),
  createSurveyFromImport: vi.fn(),
  createSurveyFromTemplate: vi.fn(),
  uploadSurveyCover: vi.fn(),
}));
vi.mock('../../../api/categoriesApi.js', () => ({ getCategories: vi.fn(() => Promise.resolve([])) }));
vi.mock('../../../components/SurveyOriginModal/SurveyOriginModal.jsx', () => ({
  SurveyOriginModal: ({ isOpen, onSelect }) => isOpen && (
    <div role="dialog" aria-label="Punto de partida">
      <button type="button" onClick={() => onSelect({ type: 'BLANK' })}>Desde cero</button>
      <button type="button" onClick={() => onSelect({ type: 'TEMPLATE', template: { id: 8, title: 'Plantilla base' } })}>Desde plantilla</button>
    </div>
  ),
}));

function CreatedSurvey() {
  const location = useLocation();
  return <h1>Encuesta creada: {location.state?.survey?.id}</h1>;
}

function renderPage(state) {
  render(
    <MemoryRouter initialEntries={[{ pathname: '/encuestas/crear', state }]}>
      <Routes>
        <Route path="/encuestas/crear" element={<CreateSurveyPage />} />
        <Route path="/encuestas/:id/editar" element={<CreatedSurvey />} />
      </Routes>
    </MemoryRouter>,
  );
}

async function chooseCategory(user, name = 'Salud') {
  await user.click(screen.getByRole('button', { name: /Categoría/ }));
  fireEvent.click(await screen.findByRole('button', { name, exact: true }));
}

describe('CreateSurveyPage', () => {
  afterEach(() => vi.unstubAllGlobals());
  beforeEach(() => {
    vi.resetAllMocks();
    getCategories.mockResolvedValue([{ code: 'OTROS', name: 'Otros' }, { code: 'SALUD', name: 'Salud', icon: 'BsPeople' }]);
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:cover-preview') });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
  });

  it('sube una portada local después de crear la encuesta', async () => {
    const user = userEvent.setup();
    const file = new File(['imagen'], 'portada.png', { type: 'image/png' });
    createSurvey.mockResolvedValue({ id: 16 });
    uploadSurveyCover.mockResolvedValue({ id: 16, imageKey: '/api/v1/public/surveys/16/cover/imagen.png' });
    renderPage({ surveyOrigin: { type: 'BLANK' } });

    await chooseCategory(user);
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta con portada');
    await user.upload(screen.getByLabelText('Imagen del dispositivo'), file);
    expect(screen.getByText('portada.png')).toBeVisible();
    expect(screen.queryByText('Ningún archivo seleccionado')).not.toBeInTheDocument();
    expect(screen.getByText('Cambiar imagen')).toBeVisible();
    expect(screen.getByRole('img', { name: 'Vista previa de la encuesta' }).closest('label')).toHaveAttribute('for', 'survey-image-file');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));

    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 16' })).toBeVisible();
    expect(uploadSurveyCover).toHaveBeenCalledWith(16, file);
    expect(createSurvey).toHaveBeenCalledTimes(1);
  });

  it('permite elegir una portada de la galería y subirla al crear la encuesta', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, blob: () => Promise.resolve(new Blob(['imagen'], { type: 'image/png' })) }));
    createSurvey.mockResolvedValue({ id: 17 });
    uploadSurveyCover.mockResolvedValue({ id: 17 });
    renderPage({ surveyOrigin: { type: 'BLANK' } });

    await chooseCategory(user);
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta con galería');
    const galleryTrigger = screen.getByRole('button', { name: 'ABRIR GALERÍA DE FOTOS' });
    expect(galleryTrigger).not.toHaveAttribute('aria-controls');
    await user.click(galleryTrigger);
    expect(screen.getByRole('group', { name: 'Galería de fotos' })).toBeVisible();
    expect(document.getElementById(galleryTrigger.getAttribute('aria-controls'))).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Galería de fotos' })).toBeVisible();
    expect(screen.getByText('Elige una foto para tu portada:')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Usar imagen: Aula' }));
    expect(await screen.findByText('aula.png')).toBeVisible();
    expect(galleryTrigger).not.toHaveAttribute('aria-controls');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));

    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 17' })).toBeVisible();
    expect(uploadSurveyCover).toHaveBeenCalledWith(17, expect.objectContaining({ name: 'aula.png', type: 'image/png' }));
  });

  it('abre el selector al pulsar la vista previa y sitúa la creación al final', async () => {
    const user = userEvent.setup();
    renderPage({ surveyOrigin: { type: 'BLANK' } });
    const input = screen.getByLabelText('Imagen del dispositivo');
    const inputClick = vi.fn();
    input.addEventListener('click', inputClick);

    await user.click(screen.getByText('Vista previa de la portada'));

    expect(inputClick).toHaveBeenCalledTimes(1);
    await user.upload(input, new File(['imagen'], 'portada.png', { type: 'image/png' }));
    const removeCover = screen.getByRole('button', { name: 'Quitar portada' });
    expect(removeCover.closest('.survey-cover-preview-container')).not.toBeNull();
    inputClick.mockClear();
    await user.click(removeCover);
    expect(inputClick).not.toHaveBeenCalled();
    expect(screen.queryByRole('img', { name: 'Vista previa de la encuesta' })).not.toBeInTheDocument();
    expect(screen.getByText('Ningún archivo seleccionado')).toBeVisible();
    expect(input).toHaveFocus();
    expect(screen.getByRole('heading', { name: 'APARIENCIA' }).closest('section')).toHaveAttribute('data-step', '05');
    const tagsSection = screen.getByRole('region', { name: 'ETIQUETAS' });
    expect(tagsSection).toHaveAttribute('data-step', '03');
    const generalSection = screen.getByRole('region', { name: 'INFORMACIÓN GENERAL' });
    const categoryButton = screen.getByRole('button', { name: /Categoría/ });
    expect(generalSection).toContainElement(categoryButton);
    expect(generalSection.nextElementSibling).toBe(tagsSection);
    expect(screen.getByLabelText(/Título de la encuesta/).compareDocumentPosition(categoryButton)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    const accessSection = screen.getByRole('heading', { name: 'ACCESO Y VISIBILIDAD' }).closest('section');
    expect(accessSection).toHaveAttribute('data-step', '06');
    expect(accessSection).toContainElement(screen.getByText(/El borrador no aparecerá en los listados/));
    expect(accessSection.compareDocumentPosition(screen.getByRole('button', { name: 'Crear encuesta' }))).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it('actualiza la vista previa al cambiar la tipografía', async () => {
    const user = userEvent.setup();
    renderPage({ surveyOrigin: { type: 'BLANK' } });
    const appearance = screen.getByRole('region', { name: 'APARIENCIA' });
    const preview = appearance.querySelector('.survey-font-preview-sample');

    expect(preview).toHaveTextContent('¿Cómo utilizas la tecnología en tu día a día?');
    await user.click(screen.getByRole('button', { name: 'Tipografía' }));
    fireEvent.click(screen.getByRole('button', { name: 'Lexend' }));

    expect(preview).toHaveAttribute('style', expect.stringContaining('Lexend'));
  });

  it('reintenta la subida sin crear una segunda encuesta', async () => {
    const user = userEvent.setup();
    const file = new File(['imagen'], 'portada.png', { type: 'image/png' });
    createSurvey.mockResolvedValue({ id: 17 });
    uploadSurveyCover.mockRejectedValueOnce(new Error('Error de red'))
      .mockResolvedValueOnce({ id: 17 });
    renderPage({ surveyOrigin: { type: 'BLANK' } });

    await chooseCategory(user);
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta');
    await user.upload(screen.getByLabelText('Imagen del dispositivo'), file);
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('La encuesta se creó, pero no se pudo subir la portada');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));

    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 17' })).toBeVisible();
    expect(createSurvey).toHaveBeenCalledTimes(1);
    expect(uploadSurveyCover).toHaveBeenCalledTimes(2);
  });

  it('exige título y categoría y crea un borrador desde cero con etiquetas normalizadas', async () => {
    const user = userEvent.setup();
    createSurvey.mockResolvedValue({ id: 12 });
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Desde cero' }));
    const categoryButton = screen.getByRole('button', { name: 'Categoría (obligatoria)' });
    expect(categoryButton).toHaveTextContent('Selecciona una categoría');
    expect(screen.queryByText(/Heredar categoría/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Introduce un título para la encuesta.');
    await user.type(screen.getByLabelText(/Título de la encuesta/), '  Hábitos digitales  ');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una categoría para la encuesta.');
    expect(categoryButton).toHaveAttribute('aria-invalid', 'true');
    expect(categoryButton).toHaveFocus();
    expect(createSurvey).not.toHaveBeenCalled();
    await chooseCategory(user);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.type(screen.getByLabelText(/Etiquetas/), '#Vida Digital');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 12' })).toBeVisible();
    expect(createSurvey).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Hábitos digitales', tags: ['vida-digital'], category: 'SALUD', accessMode: 'PUBLIC', visibility: 'UNLISTED',
    }));
  });

  it('permite elegir una categoría real al empezar desde cero', async () => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([{ code: 'OTROS', name: 'Otros' }, { code: 'SALUD', name: 'Salud', icon: 'BsPeople' }]);
    createSurvey.mockResolvedValue({ id: 18 });
    renderPage({ surveyOrigin: { type: 'BLANK' } });

    await user.click(screen.getByRole('button', { name: /Categoría/ }));
    const healthOption = await screen.findByRole('button', { name: 'Salud' });
    expect(healthOption.querySelector('.category-select-option svg')).not.toBeNull();
    fireEvent.click(healthOption);
    expect(screen.getByRole('button', { name: /Categoría/ })).toHaveTextContent('Salud');
    expect(screen.getByRole('button', { name: /Categoría/ }).querySelector('.category-select-option svg')).not.toBeNull();
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta de salud');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));

    expect(createSurvey).toHaveBeenCalledWith(expect.objectContaining({ category: 'SALUD' }));
  });

  it('exige contraseña para una encuesta protegida creada desde plantilla', async () => {
    const user = userEvent.setup();
    createSurveyFromTemplate.mockResolvedValue({ id: 13 });
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Desde plantilla' }));
    expect(screen.getByRole('button', { name: /Categoría/ })).toHaveTextContent('Selecciona una categoría');
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta privada');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una categoría para la encuesta.');
    expect(createSurveyFromTemplate).not.toHaveBeenCalled();
    await chooseCategory(user);
    await user.click(screen.getByRole('radio', { name: /PROTEGIDA CON CONTRASEÑA/ }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Introduce la contraseña de acceso');
    await user.type(screen.getByLabelText(/Contraseña de acceso/), 'secreta');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 13' })).toBeVisible();
    expect(createSurveyFromTemplate).toHaveBeenCalledWith(8, expect.objectContaining({
      accessMode: 'PASSWORD_PROTECTED', accessPassword: 'secreta', category: 'SALUD',
    }));
  });

  it('crea un borrador a partir del contenido importado', async () => {
    const user = userEvent.setup();
    const content = [{ itemType: 'QUESTION' }];
    createSurveyFromImport.mockResolvedValue({ id: 14 });
    renderPage({ importedSurvey: { title: 'Encuesta ODT', description: 'Importada', fileName: 'encuesta.odt', content } });
    expect(screen.getByRole('button', { name: /Categoría/ })).toHaveTextContent('Selecciona una categoría');
    await user.click(screen.getByRole('button', { name: 'Importar y crear borrador' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una categoría para la encuesta.');
    expect(createSurveyFromImport).not.toHaveBeenCalled();
    await chooseCategory(user);
    await user.click(screen.getByRole('button', { name: 'Importar y crear borrador' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 14' })).toBeVisible();
    expect(createSurveyFromImport).toHaveBeenCalledWith(expect.objectContaining({ title: 'Encuesta ODT' }), content);
  });

  it('mantiene el formulario visible cuando falla la creación', async () => {
    const user = userEvent.setup();
    createSurvey.mockRejectedValue(new Error('Servicio no disponible'));
    renderPage({ surveyOrigin: { type: 'BLANK' } });
    await chooseCategory(user);
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta de prueba');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(screen.getByLabelText(/Título de la encuesta/)).toHaveValue('Encuesta de prueba');
  });

  it('evita etiquetas duplicadas y permite quitarlas', async () => {
    const user = userEvent.setup();
    renderPage({ surveyOrigin: { type: 'BLANK' } });
    const tag = screen.getByLabelText(/Etiquetas/);
    await user.type(tag, '#Salud');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));
    expect(screen.getByText('#salud')).toBeVisible();
    await user.type(tag, 'salud');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Esta etiqueta ya está añadida.');
    await user.click(screen.getByRole('button', { name: 'Eliminar etiqueta salud' }));
    expect(screen.queryByText('#salud')).not.toBeInTheDocument();
  });

  it('muestra la portada, recupera su error y admite acceso de usuarios registrados', async () => {
    const user = userEvent.setup();
    createSurvey.mockResolvedValue({ id: 15 });
    renderPage({ surveyOrigin: { type: 'BLANK' } });
    await chooseCategory(user);
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta');
    const imageUrl = screen.getByRole('textbox', { name: 'URL de la imagen' });
    await user.type(imageUrl, 'https://example.test/cover.png');
    fireEvent.error(screen.getByRole('img', { name: 'Vista previa de la encuesta' }));
    expect(screen.queryByRole('img', { name: 'Vista previa de la encuesta' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: /USUARIOS REGISTRADOS/ }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(createSurvey).toHaveBeenCalledWith(expect.objectContaining({ accessMode: 'AUTHENTICATED', imageKey: 'https://example.test/cover.png' }));
  });
});
