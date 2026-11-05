import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createCategory, getCategories, updateCategoryIcon } from '../../api/categoriesApi.js';
import { CategoriesManagementPage } from './CategoriesManagementPage.jsx';
import { renderWithRouter } from '../../testing/render.jsx';

vi.mock('../../api/categoriesApi.js', () => ({
  createCategory: vi.fn(),
  getCategories: vi.fn(),
  updateCategoryIcon: vi.fn(),
}));
vi.mock('../../components/CategoryIconPicker/CategoryIconPicker.jsx', () => ({
  CategoryIconPicker: ({ value, onChange, label, disabled }) => (
    <select aria-label={label} value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)}>
      <option value="BsTag">Etiqueta</option>
      <option value="BsRobot">Robótica</option>
    </select>
  ),
}));

describe('CategoriesManagementPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    getCategories.mockResolvedValue([]);
  });

  it('muestra el estado vacío si aún no existen categorías', async () => {
    renderWithRouter(<CategoriesManagementPage />);
    expect(await screen.findByText('Todavía no hay categorías disponibles.')).toBeVisible();
  });

  it('crea una categoría con nombre recortado y la incorpora a la lista', async () => {
    const user = userEvent.setup();
    createCategory.mockResolvedValue({ code: 'NUEVA', name: 'Nueva tecnología', icon: 'BsRobot' });
    renderWithRouter(<CategoriesManagementPage />);
    await user.type(screen.getByRole('textbox', { name: 'Nombre de la categoría' }), '  Nueva tecnología  ');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Icono de la categoría' }), 'BsRobot');
    await user.click(screen.getByRole('button', { name: 'Crear categoría' }));
    expect(await screen.findByRole('status', { name: '' })).toHaveTextContent('Categoría «Nueva tecnología» creada.');
    expect(createCategory).toHaveBeenCalledWith('Nueva tecnología', 'BsRobot');
    expect(screen.getByText('Nueva tecnología')).toBeVisible();
  });

  it('actualiza el icono de una categoría personalizada', async () => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([{ code: 'NUEVA', name: 'Nueva tecnología', icon: 'BsTag' }]);
    updateCategoryIcon.mockResolvedValue({ code: 'NUEVA', name: 'Nueva tecnología', icon: 'BsRobot' });
    renderWithRouter(<CategoriesManagementPage />);
    await user.selectOptions(await screen.findByRole('combobox', { name: 'Cambiar icono de Nueva tecnología' }), 'BsRobot');
    expect(await screen.findByText('Icono actualizado.')).toBeVisible();
    expect(updateCategoryIcon).toHaveBeenCalledWith('NUEVA', 'BsRobot');
  });
});


describe('CategoriesManagementPage: errores', () => {
  it.each([new Error('Fallo de carga'), {}])('informa de errores al cargar categorías', async (error) => {
    getCategories.mockRejectedValue(error);
    renderWithRouter(<CategoriesManagementPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent(error.message || 'No se han podido cargar las categorías.');
  });
  it.each([new Error('Nombre duplicado'), {}])('conserva el formulario si falla la creación', async (error) => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([]);
    createCategory.mockRejectedValue(error);
    renderWithRouter(<CategoriesManagementPage />);
    await user.type(screen.getByRole('textbox', { name: 'Nombre de la categoría' }), 'Nueva');
    await user.click(screen.getByRole('button', { name: 'Crear categoría' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(error.message || 'No se ha podido crear la categoría.');
    expect(screen.getByRole('textbox', { name: 'Nombre de la categoría' })).toHaveValue('Nueva');
  });
  it('conserva iconos tras un error de actualización', async () => {
    const user = userEvent.setup();
    getCategories.mockResolvedValue([{ code: 'OTROS', name: 'Otros' }, { code: 'Z', name: 'Zeta', icon: 'BsTag' }, { code: 'A', name: 'Alfa', icon: 'BsTag' }]);
    updateCategoryIcon.mockRejectedValue({});
    renderWithRouter(<CategoriesManagementPage />);
    await user.selectOptions(await screen.findByRole('combobox', { name: 'Cambiar icono de Alfa' }), 'BsRobot');
    expect(await screen.findByText('No se ha podido actualizar el icono.')).toBeVisible();
    expect(screen.getByRole('combobox', { name: 'Cambiar icono de Alfa' })).toHaveValue('BsTag');
  });
});
