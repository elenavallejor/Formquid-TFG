import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdAdd, MdChevronLeft, MdChevronRight, MdOutlineBarChart, MdOutlineDescription, MdOutlineHelpOutline, MdOutlineLightbulb, MdOutlinePersonAdd, MdOutlinePoll, MdOutlineShield, MdOutlineWidgets, MdPeopleOutline, MdSearch } from 'react-icons/md';

import { getAdminDashboard } from '../../../api/dashboardApi.js';
import { getMySurveys } from '../../../api/surveysApi.js';
import { AdminPanelButton } from '../../../components/AdminPanelButton/AdminPanelButton.jsx';
import { Dropdown } from '../../../components/Dropdown/Dropdown.jsx';
import { PanelButton } from '../../../components/PanelButton/PanelButton.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { Table } from '../../../components/Table/Table.jsx';
import { useAuth } from '../../../context/AuthContext.jsx';

import './PanelPage.css';

/**
 * Muestra el panel correspondiente al rol y carga los datos necesarios para su gestión.
 */
const EDITOR_SURVEYS_PAGE_SIZE = 8;
const SEARCH_DEBOUNCE_MS = 300;

const ROLE_CONTENT = {
  EDITOR: {
    title: 'PANEL DE EDITOR',
    description: 'Desde aquí puedes crear y gestionar elementos y encuestas.',
    type: 'EDITOR',
  },
  ADMIN: {
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

const EDITOR_ACTIONS = [
  {
    title: 'Crear encuesta',
    description: 'Crea una nueva encuesta desde cero o a partir de una plantilla.',
    icon: MdAdd,
    to: '/encuestas/crear',
    buttonText: 'Crear encuesta',
  },
  {
    title: 'Colaboraciones',
    description: 'Gestiona colaboradores, solicitudes y permisos de tus encuestas.',
    icon: MdPeopleOutline,
    to: '/colaboraciones',
    buttonText: 'Ver colaboraciones',
  },
  {
    title: 'Resultados',
    description: 'Consulta y analiza los resultados de tus encuestas.',
    icon: MdOutlineBarChart,
    to: '/resultados',
    buttonText: 'Ver resultados',
  },
  {
    title: 'Sugerir elemento',
    description: 'Propón elementos que puedan ser útiles para otros editores.',
    icon: MdOutlineLightbulb,
    to: '/sugerir-elemento',
    buttonText: 'Sugerir elemento',
  },
];

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

const TABLE_HEADERS = [
  'Título',
  'Editor',
  'Ciclo de vida',
  'Modo de acceso',
  'Visibilidad',
  'Respuestas',
  'Actualización',
];

export function PanelPage() {
  const navigate = useNavigate();
  const { user, isInitializing } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [dataError, setDataError] = useState('');

  const [catalogRequestsCount, setCatalogRequestsCount] = useState(null);
  const [registrationRequestsCount, setRegistrationRequestsCount] = useState(null);
  const [surveyCount, setSurveyCount] = useState(null);
  const [adminSurveys, setAdminSurveys] = useState([]);

  const [editorSurveys, setEditorSurveys] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('RECENT');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
    }, SEARCH_DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [searchQuery]);

  useEffect(() => {
    if (!user?.id || !user?.role) return undefined;

    let isMounted = true;

    async function loadPanelData() {
      try {
        setIsLoading(true);
        setDataError('');

        if (user.role === 'EDITOR') {
          const response = await getMySurveys({
            search: debouncedSearchQuery,
            lifecycleStatus: statusFilter,
            sortOrder,
            page: currentPage,
            size: EDITOR_SURVEYS_PAGE_SIZE,
          });

          if (!isMounted) return;

          setEditorSurveys(
            Array.isArray(response?.content)
              ? response.content
              : [],
          );

          setTotalPages(
            Number.isInteger(response?.totalPages) && response.totalPages > 0
              ? response.totalPages
              : 1,
          );

          return;
        }

        if (user.role === 'ADMIN' || user.role === 'SUPERADMIN') {
          setCatalogRequestsCount(null);
          setRegistrationRequestsCount(null);
          setSurveyCount(null);
          setAdminSurveys([]);

          const dashboard = await getAdminDashboard();

          if (!isMounted) return;

          setCatalogRequestsCount(
            dashboard.pendingElementRequests ?? 0,
          );

          setRegistrationRequestsCount(
            dashboard.pendingRegistrationRequests ?? 0,
          );

          setSurveyCount(
            dashboard.surveyCount ?? 0,
          );

          setAdminSurveys(
            Array.isArray(dashboard.recentSurveys)
              ? dashboard.recentSurveys
              : [],
          );

          return;
        }

        setDataError(
          'El rol del usuario no está reconocido por la aplicación.',
        );
      } catch (error) {
        if (isMounted) {
          console.error('Error al cargar datos del panel:', error);
          setDataError(
            'No se han podido obtener los datos del panel.',
          );
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadPanelData();

    return () => {
      isMounted = false;
    };
  }, [
    user?.id,
    user?.role,
    debouncedSearchQuery,
    statusFilter,
    sortOrder,
    currentPage,
  ]);

  if (!user) {
    return (
      <main className="panel-page" aria-busy={isInitializing}>
        <div className="panel-container">
          <Skeleton variant="list" count={4} />
        </div>
      </main>
    );
  }

  const panelContent = ROLE_CONTENT[user.role];

  if (!panelContent) return null;

  const isSuperAdmin = user.role === 'SUPERADMIN';

  const tableData = adminSurveys.map((survey, index) => {
    const id = survey.id ?? index;
    const lifecycle = survey.lifecycleStatus ?? 'DRAFT';
    const accessMode = survey.accessMode ?? 'PUBLIC';
    const visibility = survey.visibility ?? 'LISTED';

    const formattedDate = survey.updatedAt
      ? new Date(survey.updatedAt).toLocaleDateString('es-ES')
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

  return (
    <main
      className={`panel-page panel-type-${panelContent.type.toLowerCase()}`}
    >
      {panelContent.type === 'EDITOR' && (
        <div className="editor-panel-wrapper">
          <section className="editor-top-section">
            <div className="panel-container">
              <div className="panel-header">
                <h1 className="panel-title">
                  {panelContent.title}
                </h1>

                <p className="panel-description">
                  {panelContent.description}
                </p>

                {dataError && (
                  <p
                    className="panel-error-message"
                    role="alert"
                  >
                    {dataError}
                  </p>
                )}
              </div>

              <div className="editor-actions-grid">
                {EDITOR_ACTIONS.map((card) => {
                  const Icon = card.icon;

                  return (
                    <article
                      key={card.title}
                      className="editor-action-card"
                    >
                      <div className="action-card-icon-circle">
                        <Icon aria-hidden="true" />
                      </div>

                      <h2 className="action-card-title">
                        {card.title}
                      </h2>

                      <p className="action-card-description">
                        {card.description}
                      </p>

                      <button
                        type="button"
                        className="action-card-btn"
                        onClick={() => navigate(card.to)}
                      >
                        {card.buttonText}
                      </button>
                    </article>
                  );
                })}
              </div>

              <div className="admin-section editor-catalog-section">
                <div className="admin-section-header-row">
                  <h2 className="admin-section-title">
                    ACCESO AL CATÁLOGO
                  </h2>

                  <Link
                    to="/catalogo"
                    className="btn-admin-action-link"
                  >
                    Ver catálogo &rarr;
                  </Link>
                </div>

                <div className="catalog-buttons-grid">
                  <PanelButton
                    title="Plantillas"
                    description="Consulta las plantillas de encuesta."
                    icon={MdOutlineDescription}
                    to="/catalogo/plantillas"
                  />
                  
                  <PanelButton
                    title="Bloques"
                    description="Consulta los bloques de preguntas reutilizables."
                    icon={MdOutlineWidgets}
                    to="/catalogo/bloques"
                  />

                  <PanelButton
                    title="Preguntas fijas"
                    description="Consulta las preguntas disponibles en el catálogo."
                    icon={MdOutlineHelpOutline}
                    to="/catalogo/preguntas"
                  />  
                  
                </div>
              </div>
            </div>
          </section>

          <section
            className="editor-surveys-section"
            aria-busy={isLoading}
          >
            <div className="panel-container">
              <div className="surveys-header-row">
                <div>
                  <h2 className="surveys-section-title">
                    Mis encuestas
                  </h2>

                  <p className="surveys-section-subtitle">
                    Encuestas que has creado o en las que colaboras.
                  </p>
                </div>

                <Link
                  to="/encuestas"
                  className="btn-see-all"
                >
                  Ver todas
                </Link>
              </div>

              <div className="surveys-filters-bar">
                <div className="surveys-search-wrapper">
                  <MdSearch
                    className="search-icon"
                    aria-hidden="true"
                  />

                  <input
                    type="search"
                    value={searchQuery}
                    placeholder="Buscar encuestas..."
                    aria-label="Buscar encuestas"
                    onChange={(event) => {
                      setSearchQuery(event.target.value);
                      setCurrentPage(0);
                    }}
                  />
                </div>

                <div className="surveys-selects-group">
                  <Dropdown
                    value={statusFilter}
                    aria-label="Filtrar encuestas por estado"
                    onChange={(event) => {
                      setStatusFilter(event.target.value);
                      setCurrentPage(0);
                    }}
                  >
                    <option value="ALL">
                      Todos los estados
                    </option>
                    <option value="OPEN">
                      Abiertas
                    </option>
                    <option value="DRAFT">
                      Borradores
                    </option>
                    <option value="CLOSED">
                      Cerradas
                    </option>
                  </Dropdown>

                  <Dropdown
                    value={sortOrder}
                    aria-label="Ordenar encuestas"
                    onChange={(event) => {
                      setSortOrder(event.target.value);
                      setCurrentPage(0);
                    }}
                  >
                    <option value="RECENT">
                      Más recientes primero
                    </option>
                    <option value="OLDEST">
                      Más antiguas primero
                    </option>
                  </Dropdown>
                </div>
              </div>

              {isLoading ? (
                <Skeleton variant="grid" count={4} />
              ) : !dataError && editorSurveys.length === 0 ? (
                <p className="panel-status">
                  No se han encontrado encuestas.
                </p>
              ) : !dataError ? (
                <>
                  <div className="surveys-cards-grid">
                    {editorSurveys.map((survey) => (
                      <SurveyCard
                        key={survey.id}
                        survey={survey}
                        onEdit={(id) =>
                          navigate(`/encuestas/${id}/editar`)
                        }
                        onView={(id) =>
                          navigate(`/encuestas/${id}`)
                        }
                      />
                    ))}
                  </div>

                  {totalPages > 1 && (
                    <div className="pagination-controls">
                      <button
                        type="button"
                        className="btn-pagination"
                        disabled={currentPage === 0}
                        onClick={() => {
                          setCurrentPage((page) =>
                            Math.max(page - 1, 0),
                          );
                        }}
                      >
                        <MdChevronLeft aria-hidden="true" />
                        Anterior
                      </button>

                      <span className="pagination-info">
                        Página{' '}
                        <strong>{currentPage + 1}</strong>
                        {' '}de{' '}
                        <strong>{totalPages}</strong>
                      </span>

                      <button
                        type="button"
                        className="btn-pagination"
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => {
                          setCurrentPage((page) =>
                            Math.min(page + 1, totalPages - 1),
                          );
                        }}
                      >
                        Siguiente
                        <MdChevronRight aria-hidden="true" />
                      </button>
                    </div>
                  )}
                </>
              ) : null}
            </div>
          </section>
        </div>
      )}

      {panelContent.type === 'ADMIN_SUPER' && (
        <section
          className="panel-container"
          aria-labelledby="panel-title"
        >
          <div className="panel-header">
            <h1
              id="panel-title"
              className="panel-title"
            >
              {panelContent.title}
            </h1>

            <p className="panel-description">
              {panelContent.description}
            </p>

            {dataError && (
              <p
                className="panel-error-message"
                role="alert"
              >
                {dataError}
              </p>
            )}
          </div>

          <div className="admin-panel-content">
            <section
              className="admin-section"
              aria-busy={isLoading}
            >
              <h2 className="admin-section-title">
                PENDIENTES DE GESTIÓN
              </h2>

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
                  count={surveyCount}
                  icon={MdOutlinePoll}
                  to="/admin/encuestas"
                  theme="teal"
                  counterLabel="creadas"
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

            {isSuperAdmin && (
              <section className="admin-section">
                <h2 className="admin-section-title">
                  OTRAS GESTIONES
                </h2>

                <div className="admin-cards-grid-full">
                  <AdminPanelButton
                    title="USUARIOS Y PERMISOS"
                    description="Gestiona usuarios y permisos en la plataforma."
                    icon={MdOutlineShield}
                    to="/superadmin/usuarios"
                    buttonLabel="Ver usuarios"
                    theme="purple"
                    fullWidth
                  />
                </div>
              </section>
            )}

            <section className="admin-section">
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">
                  ACCESO AL CATÁLOGO
                </h2>
              </div>

              <div className="catalog-buttons-grid">
                <PanelButton
                  title="Plantillas"
                  description="Gestiona las plantillas de encuestas."
                  icon={MdOutlineDescription}
                  to="/catalogo/plantillas"
                />

                <PanelButton
                  title="Bloques"
                  description="Gestiona los bloques de preguntas."
                  icon={MdOutlineWidgets}
                  to="/catalogo/bloques"
                />

                 <PanelButton
                  title="Preguntas fijas"
                  description="Gestiona las preguntas disponibles en el catálogo."
                  icon={MdOutlineHelpOutline}
                  to="/catalogo/preguntas"
                />

                
              </div>
            </section>

            <section
              className="admin-section surveys-summary-section"
              aria-busy={isLoading}
            >
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">
                  RESUMEN DE ENCUESTAS
                </h2>

                <Link
                  to="/admin/encuestas"
                  className="btn-admin-action-link"
                >
                  Ver encuestas &rarr;
                </Link>
              </div>

              {isLoading ? (
                <Skeleton variant="table" count={5} />
              ) : !dataError ? (
                <Table
                  headers={TABLE_HEADERS}
                  data={tableData}
                  emptyMessage="Todavía no hay encuestas registradas."
                />
              ) : null}
            </section>
          </div>
        </section>
      )}
    </main>
  );
}
