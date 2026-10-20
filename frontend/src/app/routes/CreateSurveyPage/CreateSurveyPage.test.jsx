import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createSurvey, createSurveyFromImport, createSurveyFromTemplate } from '../../../api/surveysApi.js';
import { CreateSurveyPage } from './CreateSurveyPage.jsx';

vi.mock('../../../api/surveysApi.js', () => ({
  createSurvey: vi.fn(),
  createSurveyFromImport: vi.fn(),
  createSurveyFromTemplate: vi.fn(),
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

describe('CreateSurveyPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('exige título y crea un borrador desde cero con etiquetas normalizadas', async () => {
    const user = userEvent.setup();
    createSurvey.mockResolvedValue({ id: 12 });
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Desde cero' }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Introduce un título para la encuesta.');
    await user.type(screen.getByLabelText(/Título de la encuesta/), '  Hábitos digitales  ');
    await user.type(screen.getByLabelText(/Etiquetas/), '#Vida Digital');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 12' })).toBeVisible();
    expect(createSurvey).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Hábitos digitales', tags: ['vida-digital'], accessMode: 'PUBLIC', visibility: 'UNLISTED',
    }));
  });

  it('exige contraseña para una encuesta protegida creada desde plantilla', async () => {
    const user = userEvent.setup();
    createSurveyFromTemplate.mockResolvedValue({ id: 13 });
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Desde plantilla' }));
    await user.type(screen.getByLabelText(/Título de la encuesta/), 'Encuesta privada');
    await user.click(screen.getByRole('radio', { name: /PROTEGIDA CON CONTRASEÑA/ }));
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Introduce la contraseña de acceso');
    await user.type(screen.getByLabelText(/Contraseña de acceso/), 'secreta');
    await user.click(screen.getByRole('button', { name: 'Crear encuesta' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 13' })).toBeVisible();
    expect(createSurveyFromTemplate).toHaveBeenCalledWith(8, expect.objectContaining({
      accessMode: 'PASSWORD_PROTECTED', accessPassword: 'secreta',
    }));
  });

  it('crea un borrador a partir del contenido importado', async () => {
    const user = userEvent.setup();
    const content = [{ itemType: 'QUESTION' }];
    createSurveyFromImport.mockResolvedValue({ id: 14 });
    renderPage({ importedSurvey: { title: 'Encuesta ODT', description: 'Importada', fileName: 'encuesta.odt', content } });
    await user.click(screen.getByRole('button', { name: 'Importar y crear borrador' }));
    expect(await screen.findByRole('heading', { name: 'Encuesta creada: 14' })).toBeVisible();
    expect(createSurveyFromImport).toHaveBeenCalledWith(expect.objectContaining({ title: 'Encuesta ODT' }), content);
  });

  it('mantiene el formulario visible cuando falla la creación', async () => {
    const user = userEvent.setup();
    createSurvey.mockRejectedValue(new Error('Servicio no disponible'));
    renderPage({ surveyOrigin: { type: 'BLANK' } });
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
