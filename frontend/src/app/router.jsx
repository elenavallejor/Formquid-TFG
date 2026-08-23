import { createBrowserRouter } from 'react-router-dom';

import { App } from './app';
import { LoginPage } from './routes/LoginPage/LoginPage';
import { MainPage } from './routes/MainPage/MainPage';
import { RegisterPage } from './routes/RegisterPage/RegisterPage';
import { PanelPage } from './routes/PanelPage/PanelPage';
import { ErrorPage } from './routes/ErrorPage/ErrorPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      {
        index: true,
        element: <MainPage />,
      },
      {
        path: 'inicia-sesion',
        element: <LoginPage />,
      },
      {
        path: 'solicita-cuenta',
        element: <RegisterPage />,
      },
      {
        path: 'panel',
        element: <PanelPage />,
      },
      {
        path: '*',
        element: (
          <ErrorPage
            code="404"
            title="Página no encontrada"
            message="La ruta a la que intentas acceder no existe o ha sido movida."
          />
        ),
      },
    ],
  },
]);