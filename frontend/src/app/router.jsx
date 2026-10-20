import { Navigate, createBrowserRouter } from 'react-router-dom';

import { RequireAuth } from './guards/RequireAuth.jsx';
import { RequireRole } from './guards/RequireRole.jsx';
import { App } from './app';
import { ElementDetailPage } from './routes/ElementDetailPage/ElementDetailPage.jsx';
import { ElementRequestsPage } from './routes/ElementRequestsPage/ElementRequestsPage.jsx';
import { CatalogPage } from './routes/CatalogPage/CatalogPage.jsx';
import { CollaborationsPage } from './routes/CollaborationsPage/CollaborationsPage.jsx';
import { CreateQuestionPage } from './routes/CreateQuestionPage/CreateQuestionPage.jsx';
import { CreateBlockPage } from './routes/CreateBlockPage/CreateBlockPage.jsx';
import { CreateTemplatePage } from './routes/CreateTemplatePage/CreateTemplatePage.jsx';
import { CreateSurveyPage } from './routes/CreateSurveyPage/CreateSurveyPage.jsx';
import { ImportSurveyPage } from './routes/ImportSurveyPage/ImportSurveyPage.jsx';
import { EditSurveyPage } from './routes/EditSurveyPage/EditSurveyPage.jsx';
import { EditElementPage } from './routes/EditElementPage/EditElementPage.jsx';
import { ErrorPage } from './routes/ErrorPage/ErrorPage';
import { LoginPage } from './routes/LoginPage/LoginPage';
import { MainPage } from './routes/MainPage/MainPage';
import { PanelPage } from './routes/PanelPage/PanelPage';
import { SurveyParticipationsPage } from './routes/SurveyParticipationsPage/SurveyParticipationsPage.jsx';
import { PublicSurveyPage } from './routes/PublicSurveyPage/PublicSurveyPage.jsx';
import { RegisterPage } from './routes/RegisterPage/RegisterPage';
import { RegistrationRequestsPage } from './routes/RegistrationRequestsPage/RegistrationRequestsPage';
import { ResultsPage } from './routes/ResultsPage/ResultsPage.jsx';
import { SuggestElementPage } from './routes/SuggestElementPage/SuggestElementPage.jsx';
import { SurveyResultsPage } from './routes/SurveyResultsPage/SurveyResultsPage.jsx';
import { AccountPage } from './routes/AccountPage/AccountPage.jsx';
import { AdminSurveysPage } from './routes/AdminSurveysPage/AdminSurveysPage.jsx';
import { SurveyListPage } from './routes/SurveyListPage/SurveyListPage.jsx';
import { UsersManagementPage } from './routes/UsersManagementPage/UsersManagementPage.jsx';
import { CategoriesManagementPage } from './routes/CategoriesManagementPage.jsx';

/**
 * Define las rutas de la aplicación y aplica las restricciones de autenticación y rol necesarias.
 */
const ADMIN_ROLES = ['ADMIN', 'SUPERADMIN'];
const EDITOR_ROLE = ['EDITOR'];
const SURVEY_MANAGEMENT_ROLES = ['EDITOR', 'ADMIN', 'SUPERADMIN'];
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
        path: '403',
        element: <ErrorPage code={403} />,
      },
      {
        path: ':publicCode',
        loader: ({ params }) => {
          if (!/^[A-Z0-9]{12}$/i.test(params.publicCode ?? '')) {
            throw new Response(null, { status: 404 });
          }
          return null;
        },
        element: <PublicSurveyPage />,
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
        path: 'encuestas',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={EDITOR_ROLE}>
              <SurveyListPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'colaboraciones',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={EDITOR_ROLE}>
              <CollaborationsPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'encuestas/crear',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={EDITOR_ROLE}>
              <CreateSurveyPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'encuestas/importar',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={EDITOR_ROLE}>
              <ImportSurveyPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'encuestas/:surveyId/editar',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SURVEY_MANAGEMENT_ROLES}>
              <EditSurveyPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'resultados',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SURVEY_MANAGEMENT_ROLES}>
              <ResultsPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'encuestas/:surveyId/resultados',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SURVEY_MANAGEMENT_ROLES}>
              <SurveyResultsPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'encuestas/:surveyId/resultados/participaciones',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SURVEY_MANAGEMENT_ROLES}>
              <SurveyParticipationsPage />
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
        path: 'catalogo/detalles/:elementType/:elementId',
        element: (
          <RequireAuth>
            <ElementDetailPage />
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
        path: 'admin/encuestas',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <AdminSurveysPage />
            </RequireRole>
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
        path: 'admin/solicitudes-catalogo',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={ADMIN_ROLES}>
              <ElementRequestsPage />
            </RequireRole>
          </RequireAuth>
        ),
      },
      {
        path: 'superadmin/categorias',
        element: (
          <RequireAuth>
            <RequireRole allowedRoles={SUPERADMIN_ROLE}>
              <CategoriesManagementPage />
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
