import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createCatalogTemplate } from '../../../api/catalogApi.js';
import { CreateTemplatePage } from './CreateTemplatePage.jsx';
import { renderWithDataRouter } from '../../../testing/render.jsx';

vi.mock('../../../api/catalogApi.js', () => ({ createCatalogTemplate: vi.fn() }));
vi.mock('../../../api/categoriesApi.js', () => ({
  getCategories: vi.fn(() => Promise.resolve([{ code: 'SALUD', name: 'Salud' }])),
}));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));

describe('CreateTemplatePage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('muestra la categoría debajo del título y la incluye al guardar', async () => {
    const user = userEvent.setup();
    createCatalogTemplate.mockResolvedValue({ id: 9 });
    renderWithDataRouter([{ path: '/', element: <CreateTemplatePage /> }]);

    const generalInfo = screen.getByRole('heading', { name: 'INFORMACIÓN GENERAL' }).closest('section');
    const title = within(generalInfo).getByRole('textbox', { name: /Título de la plantilla/ });
    const category = within(generalInfo).getByRole('button', { name: 'Categoría (obligatoria)' });
    const description = within(generalInfo).getByRole('textbox', { name: 'Descripción' });
    expect(category).toHaveTextContent('Selecciona la categoría de tu plantilla');
    expect(title.compareDocumentPosition(category) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(category.compareDocumentPosition(description) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    await user.type(title, 'Plantilla de salud');
    await user.click(category);
    fireEvent.click(await within(generalInfo).findByRole('button', { name: 'Salud' }));
    expect(category).toHaveTextContent('Salud');
    await user.click(screen.getByRole('button', { name: 'Guardar plantilla' }));
    expect(createCatalogTemplate).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Plantilla de salud',
      category: 'SALUD',
    }));
  });
});
