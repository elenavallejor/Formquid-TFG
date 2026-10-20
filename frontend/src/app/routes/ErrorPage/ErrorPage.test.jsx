import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { ErrorPage } from './ErrorPage.jsx';

function renderErrorPage(element, route = '/') {
  const router = createMemoryRouter([{ path: '*', element }], { initialEntries: [route] });
  render(<RouterProvider router={router} />);
}

describe('ErrorPage', () => {
  it('muestra el error de permisos y actualiza el título', () => {
    renderErrorPage(<ErrorPage code={403} />, '/403');
    expect(screen.getByRole('heading', { name: 'Acceso no permitido' })).toBeVisible();
    expect(screen.getByText('No tienes permisos para acceder a esta sección de FormQuid.')).toBeVisible();
    expect(document.title).toBe('Acceso no permitido | FormQuid');
  });

  it('muestra la URL solicitada para un 404', () => {
    renderErrorPage(<ErrorPage />, '/desconocida');
    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeVisible();
    expect(screen.getByText('No se ha encontrado la URL solicitada /desconocida.')).toBeVisible();
  });

  it('acepta un título y mensaje específicos', () => {
    renderErrorPage(<ErrorPage code={500} title="Fallo temporal" message="Prueba de nuevo." />);
    expect(screen.getByRole('heading', { name: 'Fallo temporal' })).toBeVisible();
    expect(screen.getByText('Prueba de nuevo.')).toBeVisible();
  });
});
