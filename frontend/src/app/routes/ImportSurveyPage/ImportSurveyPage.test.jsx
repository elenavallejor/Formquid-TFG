import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { previewSurveyImport } from '../../../api/surveysApi.js';
import { ImportSurveyPage } from './ImportSurveyPage.jsx';
import { renderWithRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/surveysApi.js', () => ({ previewSurveyImport: vi.fn() }));

function ImportDestination() {
  const location = useLocation();
  return <p>Encuesta importada: {location.state?.importedSurvey?.title}</p>;
}

describe('ImportSurveyPage', () => {
  beforeEach(() => vi.resetAllMocks());

  it('rechaza archivos con formato incorrecto o demasiado grandes', async () => {
    const { container } = renderWithRouter(<ImportSurveyPage />);
    const input = container.querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [new File(['texto'], 'notas.txt')] } });
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona un documento con formato .odt.');
    fireEvent.change(input, { target: { files: [new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.odt')] } });
    expect(screen.getByRole('alert')).toHaveTextContent('El documento no puede superar 5 MB.');
    expect(previewSurveyImport).not.toHaveBeenCalled();
  });

  it('valida un ODT y entrega la vista previa a creación', async () => {
    const user = userEvent.setup();
    const file = new File(['contenido'], 'encuesta.odt', { type: 'application/vnd.oasis.opendocument.text' });
    previewSurveyImport.mockResolvedValue({ title: 'Encuesta importada', blockCount: 1, questionCount: 2 });
    const { container } = renderWithRouter(
      <Routes>
        <Route path="/" element={<ImportSurveyPage />} />
        <Route path="/encuestas/crear" element={<ImportDestination />} />
      </Routes>,
    );
    await user.upload(container.querySelector('input[type="file"]'), file);
    await user.click(screen.getByRole('button', { name: 'Validar documento' }));
    expect(await screen.findByRole('heading', { name: 'DOCUMENTO VÁLIDO' })).toBeVisible();
    expect(previewSurveyImport).toHaveBeenCalledWith(file);
    await user.click(screen.getByRole('button', { name: 'Continuar con esta encuesta' }));
    expect(screen.getByText('Encuesta importada: Encuesta importada')).toBeVisible();
  });

  it('muestra el error de validación y permite retirar el archivo', async () => {
    const user = userEvent.setup();
    previewSurveyImport.mockRejectedValue(new Error('Plantilla inválida'));
    const { container } = renderWithRouter(<ImportSurveyPage />);
    await user.upload(container.querySelector('input[type="file"]'), new File(['contenido'], 'encuesta.odt'));
    await user.click(screen.getByRole('button', { name: 'Validar documento' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Plantilla inválida');
    await user.click(screen.getByRole('button', { name: 'Quitar encuesta.odt' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Validar documento' })).toBeDisabled();
  });

  it('acepta un documento arrastrado y permite reemplazarlo tras la vista previa', async () => {
    const user = userEvent.setup();
    const file = new File(['contenido'], 'arrastrada.odt');
    previewSurveyImport.mockResolvedValue({ title: 'Encuesta', blockCount: 2, questionCount: 3 });
    renderWithRouter(<ImportSurveyPage />);
    const dropzone = screen.getByRole('button', { name: /Arrastra aquí el documento/ });
    fireEvent.dragEnter(dropzone);
    expect(dropzone).toHaveClass('is-dragging');
    fireEvent.drop(dropzone, { dataTransfer: { files: [file] } });
    expect(dropzone).not.toHaveClass('is-dragging');
    expect(screen.getByText('arrastrada.odt')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Validar documento' }));
    expect(await screen.findByRole('heading', { name: 'DOCUMENTO VÁLIDO' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Reemplazar archivo' }));
    expect(screen.queryByText('arrastrada.odt')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Validar documento' })).toBeDisabled();
  });

  it.each([[2048, '2 KB'], [1048576, '1.0 MB']])('muestra el tamaño de un archivo de %s bytes', (size, label) => {
    const { container } = renderWithRouter(<ImportSurveyPage />);
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: [new File([new Uint8Array(size)], 'encuesta.odt')] } });
    expect(screen.getByText(label)).toBeVisible();
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: [] } });
    expect(screen.queryByText('encuesta.odt')).not.toBeInTheDocument();
  });
  it('gestiona la salida del arrastre y el error por defecto de validación', async () => {
    previewSurveyImport.mockRejectedValue({});
    const { container } = renderWithRouter(<ImportSurveyPage />);
    const dropzone = screen.getByRole('button', { name: /Arrastra aquí/ });
    fireEvent.dragEnter(dropzone);
    fireEvent.dragOver(dropzone);
    fireEvent.dragLeave(dropzone, { relatedTarget: dropzone.querySelector('strong') });
    fireEvent.dragLeave(dropzone, { relatedTarget: document.body });
    expect(dropzone).not.toHaveClass('is-dragging');
    fireEvent.drop(dropzone, { dataTransfer: { files: [] } });
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: [new File(['odt'], 'encuesta.odt')] } });
    fireEvent.click(screen.getByRole('button', { name: 'Validar documento' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se ha podido validar el documento.');
  });
  it('muestra el singular cuando el documento contiene una pregunta', async () => {
    previewSurveyImport.mockResolvedValue({ title: 'Única', blockCount: 1, questionCount: 1 });
    const { container } = renderWithRouter(<ImportSurveyPage />);
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: [new File(['odt'], 'encuesta.odt')] } });
    fireEvent.click(screen.getByRole('button', { name: 'Validar documento' }));
    expect(await screen.findByText('1 bloque · 1 pregunta')).toBeVisible();
  });
});
