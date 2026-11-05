import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';

import { createCatalogQuestion } from '../../../api/catalogApi.js';
import { renderWithDataRouter } from '../../../testing/render.jsx';
import { CreateQuestionPage } from './CreateQuestionPage.jsx';

vi.mock('../../../api/catalogApi.js', () => ({ createCatalogQuestion: vi.fn() }));
vi.mock('../../../api/categoriesApi.js', () => ({
  getCategories: vi.fn(() => Promise.resolve([{ code: 'SALUD', name: 'Salud' }])),
}));

it('marca la categoría como obligatoria y no guarda una pregunta sin ella', async () => {
  const user = userEvent.setup();
  renderWithDataRouter([{ path: '/', element: <CreateQuestionPage /> }]);
  const category = screen.getByRole('button', { name: 'Categoría (obligatoria)' });
  expect(category.closest('.category-select')).toHaveTextContent('*');
  await user.type(screen.getByPlaceholderText('Texto que verá el encuestado.'), '¿Qué opinas?');
  await user.click(screen.getByRole('button', { name: 'Crear pregunta' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una categoría');
  expect(createCatalogQuestion).not.toHaveBeenCalled();
});
