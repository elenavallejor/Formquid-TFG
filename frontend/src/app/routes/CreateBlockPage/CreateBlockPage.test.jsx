import { screen } from '@testing-library/react';
import { expect, it, vi } from 'vitest';

import { renderWithDataRouter } from '../../../testing/render.jsx';
import { CreateBlockPage } from './CreateBlockPage.jsx';

vi.mock('../../../api/categoriesApi.js', () => ({
  getCategories: vi.fn(() => Promise.resolve([{ code: 'SALUD', name: 'Salud' }])),
}));
vi.mock('../../../components/CatalogSidebar/CatalogSidebar.jsx', () => ({ CatalogSidebar: () => null }));

it('marca la categoría como obligatoria y bloquea la creación de preguntas hasta elegirla', () => {
  renderWithDataRouter([{ path: '/', element: <CreateBlockPage /> }]);
  const category = screen.getByRole('button', { name: 'Categoría (obligatoria)' });
  expect(category.closest('.category-select')).toHaveTextContent('*');
  expect(screen.getByRole('button', { name: 'Crear elemento' })).toBeDisabled();
});
