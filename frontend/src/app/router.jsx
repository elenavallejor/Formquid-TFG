import { createBrowserRouter } from 'react-router-dom';

import { App } from './app';
import { LoginPage } from './routes/LoginPage/LoginPage';
import { MainPage } from './routes/MainPage/MainPage';
import { RegisterPage } from './routes/RegisterPage/RegisterPage';
import { PanelPage } from './routes/PanelPage/PanelPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
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
    ],
  },
]);