import { render, screen, fireEvent, within } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import { BackButton } from '../BackButton/BackButton.jsx';
import { Footer } from '../Footer/Footer.jsx';
import { PasswordConfirmModal } from '../PasswordConfirmModal/PasswordConfirmModal.jsx';
import { SurveyPreviewModal } from '../SurveyPreviewModal/SurveyPreviewModal.jsx';

describe('navegación y pie de página', () => {
  it.each([[['/actual']], [['/anterior', '/actual']]])('vuelve a una ruta segura o al historial: %j', entries => {
    render(<MemoryRouter initialEntries={entries}><Routes>
      <Route path="/actual" element={<BackButton fallbackTo="/segura" />} />
      <Route path="/anterior" element={<p>Anterior</p>} />
      <Route path="/segura" element={<p>Segura</p>} />
    </Routes></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: 'Volver' }));
    expect(screen.getByText(entries.length === 1 ? 'Segura' : 'Anterior')).toBeVisible();
  });
  it('muestra enlaces al manual y contacto', () => {
    render(<Footer />);
    expect(screen.getByRole('link', { name: 'Manual de usuario' })).toHaveAttribute('href', '/manual');
    expect(screen.getByRole('link', { name: 'Contacto' })).toHaveAttribute('href', '/contact');
  });
});
describe('confirmación de contraseña', () => {
  it('no renderiza un diálogo cerrado', () => {
    render(<PasswordConfirmModal isOpen={false} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
  it.each([false, true])('gestiona visibilidad, errores y clic exterior: procesando=%s', isProcessing => {
    const onClose = vi.fn(), onClearError = vi.fn(), onConfirm = vi.fn();
    const { container } = render(<PasswordConfirmModal isOpen isProcessing={isProcessing} errorMessage="Revisa" onClose={onClose} onClearError={onClearError} onConfirm={onConfirm} />);
    const dialog = screen.getByRole('dialog');
    fireEvent.click(screen.getByRole('button', { name: /cerrar.*error/i }));
    expect(onClearError).toHaveBeenCalledOnce();
    expect(isProcessing ? dialog : screen.getByLabelText('Contraseña actual')).toHaveFocus();
    fireEvent.mouseDown(dialog);
    expect(onClose).not.toHaveBeenCalled();
    fireEvent.mouseDown(container.querySelector('.pwd-modal-overlay'));
    expect(onClose).toHaveBeenCalledTimes(isProcessing ? 0 : 1);
    fireEvent.submit(container.querySelector('form'));
    expect(onConfirm).not.toHaveBeenCalled();
    if (!isProcessing) {
      fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña actual' }));
      expect(screen.getByLabelText('Contraseña actual')).toHaveAttribute('type', 'text');
      fireEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña actual' }));
      fireEvent.change(screen.getByLabelText('Contraseña actual'), { target: { value: 'clave' } });
      fireEvent.submit(container.querySelector('form'));
      expect(onConfirm).toHaveBeenCalledWith('clave');
    }
  });
});
describe('vista previa de encuesta', () => {
  it('no muestra la vista cerrada', () => {
    render(<SurveyPreviewModal isOpen={false} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
  it('permite cerrar desde el fondo y desde la encuesta sin contenido', () => {
    const onClose = vi.fn();
    const { container } = render(<SurveyPreviewModal isOpen survey={{ title: 'Vista previa' }} onClose={onClose} />);
    const dialog = screen.getByRole('dialog');
    fireEvent.mouseDown(dialog);
    expect(onClose).not.toHaveBeenCalled();
    fireEvent.mouseDown(container.querySelector('.survey-preview-overlay'));
    expect(onClose).toHaveBeenCalledOnce();
    fireEvent.click(within(dialog).getAllByRole('button', { name: 'Cerrar previsualización' })[1]);
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
