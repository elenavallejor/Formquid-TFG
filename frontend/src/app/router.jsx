import { Navigate, createBrowserRouter } from 'react-router-dom';

import { RequireAuth } from './guards/RequireAuth.jsx';
import { RequireRole } from './guards/RequireRole.jsx';
import { App } from './app';
import { CatalogPage } from './routes/CatalogPage/CatalogPage.jsx';
import { CreateQuestionPage } from './routes/CreateQuestionPage/CreateQuestionPage.jsx';
import { CreateBlockPage } from './routes/CreateBlockPage/CreateBlockPage.jsx';
import { CreateTemplatePage } from './routes/CreateTemplatePage/CreateTemplatePage.jsx';
import { EditElementPage } from './routes/EditElementPage/EditElementPage.jsx';
import { ErrorPage } from './routes/ErrorPage/ErrorPage';
import { LoginPage } from './routes/LoginPage/LoginPage';
import { MainPage } from './routes/MainPage/MainPage';
import { PanelPage } from './routes/PanelPage/PanelPage';
import { RegisterPage } from './routes/RegisterPage/RegisterPage';
import { RegistrationRequestsPage } from './routes/RegistrationRequestsPage/RegistrationRequestsPage';
import { SuggestElementPage } from './routes/SuggestElementPage/SuggestElementPage.jsx';
import { AccountPage } from './routes/AccountPage/AccountPage.jsx';
import { UsersManagementPage } from './routes/UsersManagementPage/UsersManagementPage.jsx';

/**
 * Define las rutas de la aplicación y aplica las restricciones de autenticación y rol necesarias.
 */
const ADMIN_ROLES = ['ADMIN', 'SUPERADMIN'];
const EDITOR_ROLE = ['EDITOR'];
const SUPERADMIN_ROLE = ['SUPERADMIN'];

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
        path: 'perfil',
        element: (
          <RequireAuth>
            <AccountPage />
          </RequireAuth>
        ),
      },
      {
        path: 'panel',
        element: (
          <RequireAuth>
            <PanelPage />
          </RequireAuth>
        ),
      },
      {
        path: 'sugerir-elemento',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={EDITOR_ROLE}>
              <SuggestElementPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo',
        element: (
          <RequireAuth>
            <Navigate to="/catalogo/bloques" replace />
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo/crear/pregunta',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <CreateQuestionPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo/crear/bloque',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <CreateBlockPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo/crear/plantilla',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <CreateTemplatePage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo/editar/:elementType/:elementId',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <EditElementPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'catalogo/:section',
        element: (
          <RequireAuth>
            <CatalogPage />
          </RequireAuth>
        ),
      },
      {
        path: 'admin/solicitudes-registro',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <RegistrationRequestsPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'superadmin/usuarios',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SUPERADMIN_ROLE}>
              <UsersManagementPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: '*',
        element: <ErrorPage />,
      },
    ],
  },
]);
