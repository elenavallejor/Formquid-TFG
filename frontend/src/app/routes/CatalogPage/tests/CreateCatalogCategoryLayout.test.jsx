import { fireEvent, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { CreateBlockPage } from '../../CreateBlockPage/CreateBlockPage.jsx';
import { CreateQuestionPage } from '../../CreateQuestionPage/CreateQuestionPage.jsx';
import { CreateTemplatePage } from '../../CreateTemplatePage/CreateTemplatePage.jsx';
import { renderWithRouter } from '../../../../testing/render.jsx';

vi.mock('../../../../api/categoriesApi.js', () => ({
  getCategories: vi.fn(() => Promise.resolve([{ code: 'SALUD', name: 'Salud' }])),
}));
vi.mock('../../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));

describe('información general de los elementos del catálogo', () => {
  it.each([
    ['pregunta fija', CreateQuestionPage, /Enunciado/, 'Nota (opcional)'],
    ['bloque', CreateBlockPage, /Nombre del bloque/, 'Descripción'],
    ['plantilla', CreateTemplatePage, /Título de la plantilla/, 'Descripción'],
  ])('sitúa la categoría entre los campos en %s', async (element, Page, primaryLabel, finalLabel) => {
    renderWithRouter(<Page />);

    const generalInfo = screen.getByRole('heading', { name: 'INFORMACIÓN GENERAL' }).closest('section');
    const fields = generalInfo.querySelector('.builder-general-info-fields');
    const primary = within(fields).getByRole('textbox', { name: primaryLabel });
    const category = within(fields).getByRole('button', { name: 'Categoría' });
    const final = within(fields).getByRole('textbox', { name: finalLabel });

    expect(fields.children).toHaveLength(3);
    expect(category).toHaveTextContent(`Selecciona la categoría de tu ${element}`);
    expect(primary.compareDocumentPosition(category) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(category.compareDocumentPosition(final) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    fireEvent.click(category);
    fireEvent.click(await within(fields).findByRole('button', { name: 'Salud' }));
    expect(category).toHaveTextContent('Salud');
  });
});
