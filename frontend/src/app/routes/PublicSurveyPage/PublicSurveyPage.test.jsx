import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthContext } from '../../../context/authContext.js';
import { PublicSurveyPage } from './PublicSurveyPage.jsx';
import { accessPasswordProtectedSurvey, getPublicSurveyById, getRegisteredSurveyByCode } from '../../../api/surveysApi.js';
import { submitPublicParticipation } from '../../../api/participationApi.js';

vi.mock('../../../api/surveysApi.js', () => ({
  accessPasswordProtectedSurvey: vi.fn(),
  getPublicSurveyById: vi.fn(),
  getRegisteredSurveyByCode: vi.fn(),
}));
vi.mock('../../../api/participationApi.js', () => ({
  submitAuthenticatedParticipation: vi.fn(),
  submitPublicParticipation: vi.fn(),
}));
vi.mock('../../../components/Survey/Survey.jsx', () => ({
  Survey: ({ onSubmit }) => <button type="button" onClick={() => onSubmit([{ questionId: 1, textValue: 'Sí' }])}>Enviar respuestas</button>,
}));

function renderSurvey(auth = { isAuthenticated: false, isAuthLoading: false }) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/ABCDEF234567']}>
        <Routes>
          <Route path="/:publicCode" element={<PublicSurveyPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('PublicSurveyPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('presenta un error de carga con la ilustración y permite reintentar', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockRejectedValue(new Error('Red no disponible'));
    renderSurvey();

    const error = await screen.findByRole('alert');
    expect(error).toHaveTextContent('No hemos podido abrir la encuesta');
    expect(error.querySelector('img')).toHaveAttribute('src', expect.stringContaining('error-page-background'));
    expect(screen.getByRole('link', { name: 'Volver a las encuestas' })).toHaveAttribute('href', '/');

    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No hemos podido abrir la encuesta');
    expect(getPublicSurveyById).toHaveBeenCalledTimes(2);
  });

  it('solicita la contraseña cuando la encuesta está protegida', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockResolvedValue({ accessMode: 'PASSWORD_PROTECTED' });
    renderSurvey();
    expect(await screen.findByRole('heading', { name: 'Encuesta protegida' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Acceder a la encuesta' }));
    expect(screen.getByText('Escribe la contraseña de la encuesta.')).toBeVisible();
  });

  it('permite mostrar y volver a ocultar la contraseña de la encuesta', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockResolvedValue({ accessMode: 'PASSWORD_PROTECTED' });
    renderSurvey();

    const password = await screen.findByLabelText('Contraseña de la encuesta');
    expect(password).toHaveAttribute('type', 'password');
    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña de la encuesta' }));
    expect(password).toHaveAttribute('type', 'text');
    await user.click(screen.getByRole('button', { name: 'Ocultar contraseña de la encuesta' }));
    expect(password).toHaveAttribute('type', 'password');
  });

  it('elige la petición para personas autenticadas', async () => {
    getRegisteredSurveyByCode.mockRejectedValue(new Error('Red no disponible'));
    renderSurvey({ isAuthenticated: true, isAuthLoading: false });
    await screen.findByRole('alert');
    expect(getRegisteredSurveyByCode).toHaveBeenCalledWith('ABCDEF234567');
    expect(getPublicSurveyById).not.toHaveBeenCalled();
  });

  it('desbloquea una encuesta protegida con una contraseña válida', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockResolvedValue({ accessMode: 'PASSWORD_PROTECTED' });
    accessPasswordProtectedSurvey.mockResolvedValue({ title: 'Encuesta privada', accessMode: 'PASSWORD_PROTECTED' });
    renderSurvey();
    await screen.findByRole('heading', { name: 'Encuesta protegida' });
    await user.type(screen.getByLabelText('Contraseña de la encuesta'), 'secreta');
    await user.click(screen.getByRole('button', { name: 'Acceder a la encuesta' }));
    expect(await screen.findByRole('button', { name: 'Enviar respuestas' })).toBeVisible();
    expect(accessPasswordProtectedSurvey).toHaveBeenCalledWith('ABCDEF234567', 'secreta');
  });

  it('envía respuestas y muestra la confirmación', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockResolvedValue({ title: 'Encuesta abierta', accessMode: 'PUBLIC' });
    submitPublicParticipation.mockResolvedValue({ id: 7 });
    renderSurvey();
    await user.click(await screen.findByRole('button', { name: 'Enviar respuestas' }));
    expect(await screen.findByRole('heading', { name: '¡Respuestas enviadas!' })).toBeVisible();
    expect(submitPublicParticipation).toHaveBeenCalledWith('ABCDEF234567', {
      accessPassword: null,
      answers: [{ questionId: 1, textValue: 'Sí' }],
    });
  });

  it('avisa si la persona ya había participado y permite cerrar el aviso', async () => {
    const user = userEvent.setup();
    getPublicSurveyById.mockResolvedValue({ title: 'Encuesta abierta', accessMode: 'PUBLIC' });
    submitPublicParticipation.mockRejectedValue({ status: 409, data: { code: 'PARTICIPATION_ALREADY_SUBMITTED' } });
    renderSurvey();
    await user.click(await screen.findByRole('button', { name: 'Enviar respuestas' }));
    expect(await screen.findByRole('heading', { name: 'Ya has participado en esta encuesta' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar aviso de participación anterior' }));
    expect(screen.queryByRole('heading', { name: 'Ya has participado en esta encuesta' })).not.toBeInTheDocument();
  });
});
