import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { SurveyOriginModal } from './SurveyOriginModal.jsx';
import { getCatalogTemplates } from '../../api/catalogApi.js';

vi.mock('../../api/catalogApi.js', () => ({ getCatalogTemplates: vi.fn() }));

function renderModal(props = {}) {
  const onClose = vi.fn();
  const onImport = vi.fn();
  const onSelect = vi.fn();
  render(<SurveyOriginModal isOpen onClose={onClose} onImport={onImport} onSelect={onSelect} {...props} />);
  return { onClose, onImport, onSelect };
}

describe('SurveyOriginModal', () => {
  beforeEach(() => vi.resetAllMocks());

  it('permite comenzar en blanco o importar un documento', async () => {
    const user = userEvent.setup();
    const { onSelect, onImport } = renderModal();
    await user.click(screen.getByRole('button', { name: /Empezar desde cero/ }));
    expect(onSelect).toHaveBeenCalledWith({ type: 'BLANK' });
    await user.click(screen.getByRole('button', { name: /Importar un archivo ODT/ }));
    expect(onImport).toHaveBeenCalledOnce();
  });

  it('carga plantillas, filtra por texto y entrega la seleccionada', async () => {
    const user = userEvent.setup();
    getCatalogTemplates.mockResolvedValue({ content: [
      { id: 1, title: 'Salud', description: 'Bienestar' },
      { id: 2, title: 'Educación', description: 'Aprendizaje' },
    ] });
    const { onSelect } = renderModal();
    await user.click(screen.getByRole('button', { name: /Usar una plantilla del catálogo/ }));
    expect(await screen.findByRole('button', { name: /Salud/ })).toBeVisible();
    expect(getCatalogTemplates).toHaveBeenCalledWith({ page: 0, size: 100 });
    await user.type(screen.getByRole('searchbox', { name: 'Buscar plantillas' }), 'salud');
    expect(screen.queryByRole('button', { name: /Educación/ })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Salud/ }));
    expect(onSelect).toHaveBeenCalledWith({ type: 'TEMPLATE', template: { id: 1, title: 'Salud', description: 'Bienestar' } });
  });

  it('muestra el fallo de carga y permite volver a la selección inicial', async () => {
    const user = userEvent.setup();
    getCatalogTemplates.mockRejectedValue(new Error('Red caída'));
    const { onClose } = renderModal();
    await user.click(screen.getByRole('button', { name: /Usar una plantilla del catálogo/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar las plantillas');
    await user.click(screen.getByRole('button', { name: 'Volver' }));
    expect(screen.getByRole('heading', { name: '¿CÓMO QUIERES EMPEZAR?' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cancelar creación de encuesta' }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
