import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { App } from '../app.jsx';
import { router } from '../router.jsx';
import { renderWithDataRouter } from '../../testing/render.jsx';

vi.mock('../../components/Header/Header', () => ({ Header: () => <header>Cabecera</header> }));
vi.mock('../../components/Footer/Footer', () => ({ Footer: () => <footer>Pie</footer> }));

describe('navegación de la aplicación', () => {
  it('actualiza el título y lleva el foco al contenido al cambiar de pantalla', async () => {
    const user = userEvent.setup();
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
    renderWithDataRouter([{ path: '/', element: <App />, children: [
      { index: true, element: <Link to="/inicia-sesion">Iniciar sesión</Link> },
      { path: 'inicia-sesion', element: <h1>Inicio de sesión</h1> },
    ] }]);

    expect(document.title).toBe('Inicio | FormQuid');
    expect(screen.getByRole('link', { name: 'Saltar al contenido principal' })).toHaveAttribute('href', '#main-content');
    await user.click(screen.getByRole('link', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Inicio de sesión' })).toBeVisible();
    await waitFor(() => expect(document.title).toBe('Iniciar sesión | FormQuid'));
    expect(document.getElementById('main-content')).toHaveFocus();
    expect(scrollTo).toHaveBeenCalledWith(0, 0);
  });

  it('muestra un título descriptivo para rutas dinámicas y desconocidas', async () => {
    const { router: testRouter } = renderWithDataRouter([{ path: '/', element: <App />, children: [
      { path: '*', element: <p>Contenido</p> },
    ] }], { route: '/encuestas/12/editar' });
    expect(document.title).toBe('Gestionar encuesta | FormQuid');
    await testRouter.navigate('/ruta-inexistente');
    await waitFor(() => expect(document.title).toBe('Página no encontrada | FormQuid'));
  });

  it('define las rutas públicas y protege las privadas con una página de error', () => {
    const root = router.routes[0];
    const routes = root.children;
    expect(root.errorElement).toBeTruthy();
    expect(routes.find(({ path }) => path === 'inicia-sesion')).toBeTruthy();
    expect(routes.find(({ path }) => path === 'perfil')).toBeTruthy();
    expect(routes.find(({ path }) => path === 'admin/encuestas')).toBeTruthy();
    const publicSurvey = routes.find(({ path }) => path === ':publicCode');
    expect(publicSurvey.loader({ params: { publicCode: 'ABC123DEF456' } })).toBeNull();
    expect(() => publicSurvey.loader({ params: { publicCode: 'invalid' } })).toThrow();
  });
});
