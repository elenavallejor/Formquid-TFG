import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  MdOutlineLightbulb,
  MdOutlinePersonAdd,
  MdOutlineShield,
  MdOutlinePoll,
  MdOutlineHelpOutline,
  MdOutlineWidgets,
  MdOutlineDescription,
} from 'react-icons/md';

import { useAuth } from '../../../context/AuthContext.jsx';
import { AdminPanelButton } from '../../../components/AdminPanelButton/AdminPanelButton.jsx';
import { PanelButton } from '../../../components/PanelButton/PanelButton.jsx';
import { Table } from '../../../components/Table/Table.jsx';

import { getPendingRegistrationRequests } from '../../../api/registrationRequestsApi.js';
import { getPendingElementRequests } from '../../../api/elementRequestsApi.js';
import { getAdminSurveysSummary } from '../../../api/surveysApi.js';

import './PanelPage.css';

const ROLE_CONTENT = {
  EDITOR: {
    title: 'Panel de editor',
    description: 'Gestiona tus encuestas, colaboraciones y resultados',
    type: 'EDITOR',
  },
  ADMINISTRADOR: {
    title: 'PANEL DE ADMINISTRADOR',
    description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.',
    type: 'ADMIN_SUPER',
  },
  SUPERADMIN: {
    title: 'PANEL DE SUPERADMINISTRADOR',
    description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.',
    type: 'ADMIN_SUPER',
  },
};

// Diccionarios de mapeo para los badges visuales de la tabla
const LIFECYCLE_LABELS = {
  DRAFT: 'Borrador',
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
};

const ACCESS_MODE_LABELS = {
  PUBLIC: 'Pública',
  PASSWORD_PROTECTED: 'Con contraseña',
  AUTHENTICATED: 'Usuarios autenticados',
};

const VISIBILITY_LABELS = {
  LISTED: 'Listada',
  UNLISTED: 'No listada',
};

export function PanelPage() {
  const navigate = useNavigate();
  const { restoreSession, user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [dataError, setDataError] = useState(null);

  // Estado para las métricas de las tarjetas de administración
  const [catalogRequestsCount, setCatalogRequestsCount] = useState(0);
  const [registrationRequestsCount, setRegistrationRequestsCount] = useState(0);
  const [surveys, setSurveys] = useState([]);

  useEffect(() => {
    let isMounted = true;

    async function initializePanel() {
      try {
        const currentUser = await restoreSession();

        if (!isMounted) return;

        if (!currentUser) {
          navigate('/inicia-sesion', { replace: true });
          return;
        }

        const role = currentUser.role;
        const isAdminOrSuper = role === 'ADMINISTRADOR' || role === 'SUPERADMIN';

        // Peticiones paralelas solo si el usuario cuenta con permisos administrativos
        if (isAdminOrSuper) {
          const [catalogRes, registrationRes, surveysRes] = await Promise.allSettled([
            getPendingElementRequests(),
            getPendingRegistrationRequests(),
            getAdminSurveysSummary(),
          ]);

          if (isMounted) {
            // Manejo seguro de respuestas (soporta arrays planos y respuestas paginadas backend)
            if (catalogRes.status === 'fulfilled') {
              const res = catalogRes.value;
              const items = Array.isArray(res) ? res : res?.content || res?.data || [];
              setCatalogRequestsCount(items.length);
            }

            if (registrationRes.status === 'fulfilled') {
              const res = registrationRes.value;
              const items = Array.isArray(res) ? res : res?.content || res?.data || [];
              setRegistrationRequestsCount(items.length);
            }

            if (surveysRes.status === 'fulfilled') {
              const res = surveysRes.value;
              const surveyList = Array.isArray(res) ? res : res?.content || res?.data || [];
              setSurveys(surveyList);
            }
          }
        }
      } catch (error) {
        if (isMounted) {
          console.error('Error al cargar la información del panel:', error);
          setDataError('No se han podido obtener algunos datos del panel.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void initializePanel();

    return () => {
      isMounted = false;
    };
  }, [navigate, restoreSession]);

  const panelContent = useMemo(
    () => ROLE_CONTENT[user?.role] ?? null,
    [user?.role]
  );

  const isSuperAdmin = user?.role === 'SUPERADMIN';

  const tableHeaders = [
    'Título',
    'Editor',
    'Ciclo de vida',
    'Modo de acceso',
    'Visibilidad',
    'Respuestas',
    'Actualización',
  ];

  // Transformación de los datos recibidos de la API al formato consumible por <Table />
  const tableData = useMemo(() => {
    return surveys.slice(0, 5).map((survey, index) => {
      const id = survey.id ?? index;
      const lifecycle = survey.lifecycleStatus || survey.lifecycle || 'DRAFT';
      const accessMode = survey.accessMode ?? 'PUBLIC';
      const visibility = survey.visibility ?? 'LISTED';

      const formattedDate = survey.updatedAt
        ? new Date(survey.updatedAt).toLocaleDateString()
        : '-';

      return [
        survey.title ?? 'Sin título',
        survey.editorName || 'Sistema',
        <span
          key={`lifecycle-${id}`}
          className={`table-badge table-badge-${lifecycle.toLowerCase()}`}
        >
          {LIFECYCLE_LABELS[lifecycle] ?? lifecycle}
        </span>,
        <span
          key={`access-${id}`}
          className={`table-badge table-badge-${accessMode.toLowerCase()}`}
        >
          {ACCESS_MODE_LABELS[accessMode] ?? accessMode}
        </span>,
        <span
          key={`visibility-${id}`}
          className={`table-badge table-badge-${visibility.toLowerCase()}`}
        >
          {VISIBILITY_LABELS[visibility] ?? visibility}
        </span>,
        survey.responses ?? 0,
        formattedDate,
      ];
    });
  }, [surveys]);

  if (isLoading) {
    return (
      <main className="panel-page" aria-busy="true">
        <p className="panel-status">Cargando datos del panel...</p>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  if (!panelContent) {
    return (
      <main className="panel-page">
        <section className="panel-container">
          <h1>Rol de usuario no reconocido</h1>
          <p className="panel-description">
            Tu cuenta existe, pero el rol asociado ({user.role}) no tiene un panel configurado.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className={`panel-page panel-type-${panelContent.type.toLowerCase()}`}>
      <section className="panel-container" aria-labelledby="panel-title">
        <div className="panel-header">
          <h1 id="panel-title" className="panel-title">
            {panelContent.title}
          </h1>
          <p className="panel-description">{panelContent.description}</p>
          {dataError && <p className="panel-error-message">{dataError}</p>}
        </div>

        {panelContent.type === 'ADMIN_SUPER' && (
          <div className="admin-panel-content">
            {/* Sección de Pendientes y Métricas */}
            <section className="admin-section">
              <h2 className="admin-section-title">PENDIENTES DE GESTIÓN</h2>
              <div className="admin-cards-grid-3">
                <AdminPanelButton
                  title="SOLICITUDES PARA CATÁLOGO"
                  description="Revisa y gestiona las propuestas de nuevos elementos enviadas por los editores."
                  elementName="solicitudes"
                  count={catalogRequestsCount}
                  icon={MdOutlineLightbulb}
                  to="/admin/solicitudes-catalogo"
                  theme="teal"
                  counterLabel="pendientes"
                />

                <AdminPanelButton
                  title="ENCUESTAS CREADAS"
                  description="Gestiona y supervisa todas las encuestas registradas en la plataforma."
                  elementName="encuestas"
                  count={surveys.length}
                  icon={MdOutlinePoll}
                  to="/admin/encuestas"
                  theme="teal"
                  counterLabel="creadas" // Etiqueta personalizada para encuestas
                />

                <AdminPanelButton
                  title="SOLICITUDES DE REGISTRO"
                  description="Revisa las solicitudes de alta de nuevos editores."
                  elementName="registros"
                  count={registrationRequestsCount}
                  icon={MdOutlinePersonAdd}
                  to="/admin/solicitudes-registro"
                  theme="teal"
                  counterLabel="pendientes"
                />
              </div>
            </section>

            {/* Sección exclusiva para el rol SUPERADMIN */}
            {isSuperAdmin && (
              <section className="admin-section">
                <h2 className="admin-section-title">
                  OTRAS GESTIONES (SOLO SUPERADMINISTRADOR)
                </h2>
                <div className="admin-cards-grid-full">
                  <AdminPanelButton
                    title="USUARIOS Y PERMISOS"
                    description="Gestiona usuarios y permisos en la plataforma."
                    icon={MdOutlineShield}
                    to="/superadmin/usuarios"
                    buttonLabel="Ver registros"
                    theme="purple"
                    fullWidth={true}
                  />
                </div>
              </section>
            )}

            {/* Acceso directo al Catálogo */}
            <section className="admin-section">
              <h2 className="admin-section-title">Acceso al catálogo</h2>
              <div className="catalog-buttons-grid">
                <PanelButton
                  title="Preguntas fijas"
                  description="Gestiona las preguntas disponibles en el catálogo."
                  icon={MdOutlineHelpOutline}
                  to="/catalogo/preguntas"
                />

                <PanelButton
                  title="Bloques"
                  description="Gestiona los bloques de preguntas reutilizables."
                  icon={MdOutlineWidgets}
                  to="/catalogo/bloques"
                />

                <PanelButton
                  title="Plantillas"
                  description="Gestiona las plantillas de encuesta."
                  icon={MdOutlineDescription}
                  to="/catalogo/plantillas"
                />
              </div>
            </section>

            {/* Resumen dinámico en formato tabla */}
            <section className="admin-section surveys-summary-section">
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">RESUMEN DE ENCUESTAS</h2>
                <Link to="/admin/encuestas" className="btn-admin-action-link">
                  Ver encuestas &rarr;
                </Link>
              </div>

              <Table headers={tableHeaders} data={tableData} />
            </section>
          </div>
        )}
      </section>
    </main>
  );
}