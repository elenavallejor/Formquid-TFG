import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaRegClipboard } from 'react-icons/fa';
import { MdAdd, MdOutlineBarChart, MdOutlineDescription, MdOutlineHelpOutline, MdOutlineLightbulb, MdOutlinePersonAdd, MdOutlineRemoveRedEye, MdOutlineShield, MdOutlineWidgets, MdPeopleOutline } from 'react-icons/md';
import { TbCategoryPlus } from 'react-icons/tb';

import { getAdminDashboard } from '../../../api/dashboardApi.js';
import { getCategories } from '../../../api/categoriesApi.js';
import { getAdminSurveys, getMySurveys } from '../../../api/surveysApi.js';
import { AdminPanelButton } from '../../../components/AdminPanelButton/AdminPanelButton.jsx';
import { Pagination } from '../../../components/Pagination/Pagination.jsx';
import { PanelButton } from '../../../components/PanelButton/PanelButton.jsx';
import { SearchToolbar } from '../../../components/SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../../../components/Skeleton/Skeleton.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { SurveyOriginModal } from '../../../components/SurveyOriginModal/SurveyOriginModal.jsx';
import { Table } from '../../../components/Table/Table.jsx';
import { useAuth } from '../../../context/useAuth.js';

import './PanelPage.css';

/**
 * Muestra el panel correspondiente al rol y carga los datos necesarios para su gestión.
 */
const EDITOR_SURVEYS_PAGE_SIZE = 9;
const ADMIN_SURVEYS_PAGE_SIZE = 5;
const SEARCH_DEBOUNCE_MS = 300;
const ACCESS_FILTER_OPTIONS = [
  { value: 'PUBLIC', label: 'Públicas' },
  { value: 'PASSWORD_PROTECTED', label: 'Con contraseña' },
  { value: 'AUTHENTICATED', label: 'Para usuarios registrados' },
];

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
    description: 'Crea una encuesta desde cero, usa el catálogo o importa un documento ODT.',
    icon: MdAdd,
    action: 'CREATE_SURVEY',
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
  DRAFT: 'BORRADOR',
  OPEN: 'ABIERTA',
  CLOSED: 'CERRADA',
};

const ACCESS_MODE_LABELS = {
  PUBLIC: 'PÚBLICO',
  PASSWORD_PROTECTED: 'CON CONTRASEÑA',
  AUTHENTICATED: 'USUARIOS AUTENTICADOS',
};

const VISIBILITY_LABELS = {
  LISTED: 'LISTADA',
  UNLISTED: 'OCULTA',
};

const TABLE_HEADERS = [
  'Título',
  'Editor',
  'Ciclo de vida',
  'Modo de acceso',
  'Visibilidad',
  'Respuestas',
  'Actualización',
  'Acciones',
];

export function PanelPage() {
  const navigate = useNavigate();
  const { user, isInitializing } = useAuth();
  const createSurveyTriggerRef = useRef(null);

  const [isLoading, setIsLoading] = useState(true);
  const [dataError, setDataError] = useState('');
  const [isSurveyOriginOpen, setIsSurveyOriginOpen] = useState(false);

  const [catalogRequestsCount, setCatalogRequestsCount] = useState(null);
  const [registrationRequestsCount, setRegistrationRequestsCount] = useState(null);
  const [surveyCount, setSurveyCount] = useState(null);
  const [adminSurveys, setAdminSurveys] = useState([]);
  const [adminSurveysPage, setAdminSurveysPage] = useState(0);
  const [adminSurveysTotalItems, setAdminSurveysTotalItems] = useState(0);
  const [adminSurveysTotalPages, setAdminSurveysTotalPages] = useState(0);
  const [isAdminSurveysLoading, setIsAdminSurveysLoading] = useState(true);
  const [adminSurveysError, setAdminSurveysError] = useState('');

  const [editorSurveys, setEditorSurveys] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedAccessModes, setSelectedAccessModes] = useState(() => ACCESS_FILTER_OPTIONS.map(({ value }) => value));
  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
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
    if (user?.role !== 'EDITOR') return undefined;
    let active = true;
    getCategories({ surveyOnly: true })
      .then((response) => {
        if (active) setCategories(Array.isArray(response) ? response : []);
      })
      .catch(() => {});
    return () => { active = false; };
  }, [user?.role]);

  useEffect(() => {
    if (!user?.id || !user?.role) return undefined;

    let isMounted = true;

    async function loadPanelData() {
      try {
        setIsLoading(true);
        setDataError('');

        if (user.role === 'EDITOR') {
          const response = selectedAccessModes.length === 0
            ? { content: [], totalPages: 0 }
            : await getMySurveys({
                search: debouncedSearchQuery,
                lifecycleStatus: statusFilter,
                accessModes: selectedAccessModes,
                categories: selectedCategories,
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
    selectedAccessModes,
    selectedCategories,
    sortOrder,
    currentPage,
  ]);

  useEffect(() => {
    if (!user?.id || !['ADMIN', 'SUPERADMIN'].includes(user.role)) return undefined;

    let isActive = true;
    const abortController = new AbortController();
    const request = getAdminSurveys({
      page: adminSurveysPage,
      size: ADMIN_SURVEYS_PAGE_SIZE,
      signal: abortController.signal,
    });

    Promise.resolve()
      .then(() => {
        if (isActive) {
          setIsAdminSurveysLoading(true);
          setAdminSurveysError('');
        }
        return request;
      })
      .then((response) => {
        if (!isActive) return;
        setAdminSurveys(Array.isArray(response?.content) ? response.content : []);
        setAdminSurveysTotalItems(Number(response?.totalElements ?? 0));
        setAdminSurveysTotalPages(Number(response?.totalPages ?? 0));
      })
      .catch((error) => {
        if (!isActive || error.name === 'AbortError') return;
        setAdminSurveys([]);
        setAdminSurveysTotalItems(0);
        setAdminSurveysTotalPages(0);
        setAdminSurveysError(error.message || 'No se ha podido cargar el resumen de encuestas.');
      })
      .finally(() => {
        if (isActive) setIsAdminSurveysLoading(false);
      });

    return () => {
      isActive = false;
      abortController.abort();
    };
  }, [adminSurveysPage, user?.id, user?.role]);

  if (!user) {
    return (
      <main className="panel-page" aria-busy={isInitializing}>
        <div className="panel-container">
          <h1 className="sr-only">Cargando panel</h1>
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
      <button
        key={`details-${id}`}
        type="button"
        className="table-details-button"
        aria-label={`Ver detalles de ${survey.title || 'la encuesta'}`}
        onClick={() => navigate(`/encuestas/${id}/editar?vista=detalles`)}
      >
        <MdOutlineRemoveRedEye aria-hidden="true" />
        <span>Ver detalles</span>
      </button>,
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
                    <div
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
                        ref={card.action === 'CREATE_SURVEY' ? createSurveyTriggerRef : undefined}
                        type="button"
                        className="action-card-btn"
                        onClick={() => {
                          if (card.action === 'CREATE_SURVEY') {
                            setIsSurveyOriginOpen(true);
                            return;
                          }
                          navigate(card.to);
                        }}
                      >
                        {card.buttonText}
                      </button>
                    </div>
                  );
                })}
              </div>

              <div className="admin-section editor-catalog-section">
                <div className="admin-section-header-row">
                  <h2 className="admin-section-title">
                    ACCESO AL CATÁLOGO
                  </h2>
                </div>

                <ul className="catalog-buttons-grid" role="list">
                  <li>
                    <PanelButton
                      title="Plantillas"
                      description="Consulta las plantillas de encuesta."
                      icon={MdOutlineDescription}
                      to="/catalogo/plantillas"
                    />
                  </li>

                  <li>
                    <PanelButton
                      title="Bloques"
                      description="Consulta los bloques de preguntas reutilizables."
                      icon={MdOutlineWidgets}
                      to="/catalogo/bloques"
                    />
                  </li>

                  <li>
                    <PanelButton
                      title="Preguntas fijas"
                      description="Consulta las preguntas disponibles en el catálogo."
                      icon={MdOutlineHelpOutline}
                      to="/catalogo/preguntas"
                    />
                  </li>
                </ul>
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
                    MIS ENCUESTAS
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

              <SearchToolbar
                ariaLabel="Búsqueda y filtros de mis encuestas"
                searchLabel="Buscar mis encuestas"
                searchPlaceholder="Buscar"
                searchValue={searchQuery}
                onSearchChange={(event) => {
                  setSearchQuery(event.target.value);
                  setCurrentPage(0);
                }}
                filterOptions={ACCESS_FILTER_OPTIONS}
                filterOptionsLabel="Tipo de acceso"
                selectedFilterValues={selectedAccessModes}
                filterChoices={[
                  {
                    label: 'Estado de la encuesta',
                    control: 'select',
                    options: [
                      { value: 'ALL', label: 'Todos los estados' },
                      { value: 'OPEN', label: 'Abiertas' },
                      { value: 'DRAFT', label: 'Borradores' },
                      { value: 'CLOSED', label: 'Cerradas' },
                    ],
                    value: statusFilter,
                    resetValue: 'ALL',
                  },
                  {
                    label: 'Categoría',
                    control: 'multiselect',
                    allLabel: 'Todas las categorías',
                    selectedCountLabel: 'categorías seleccionadas',
                    options: categories.map(({ code, name }) => ({ value: code, label: name })),
                    value: selectedCategories,
                    resetValue: [],
                  },
                ]}
                onFilterChange={(accessModes, _text, selectedChoices) => {
                  setSelectedAccessModes(accessModes);
                  setStatusFilter(selectedChoices[0]);
                  setSelectedCategories(selectedChoices[1]);
                  setCurrentPage(0);
                }}
                onFilterReset={() => {
                  setSearchQuery('');
                  setDebouncedSearchQuery('');
                  setCurrentPage(0);
                }}
                sortAriaLabel="Ordenar mis encuestas"
                sortOptions={[
                  { value: 'RECENT', label: 'Más recientes primero' },
                  { value: 'OLDEST', label: 'Más antiguas primero' },
                ]}
                sortValue={sortOrder}
                onSortChange={(event) => {
                  setSortOrder(event.target.value);
                  setCurrentPage(0);
                }}
              />

              {isLoading ? (
                <Skeleton variant="grid" count={3} />
              ) : !dataError && editorSurveys.length === 0 ? (
                <div className="editor-surveys-empty" role="status">
                  <FaRegClipboard aria-hidden="true" />
                  <p>{selectedAccessModes.length === 0
                    ? 'Selecciona al menos un tipo de encuesta para ver los resultados.'
                    : 'No se han encontrado encuestas.'}</p>
                </div>
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
                      />
                    ))}
                  </div>

                  {totalPages > 1 && (
                    <Pagination
                      ariaLabel="Paginación de mis encuestas"
                      currentPage={currentPage}
                      onPageChange={setCurrentPage}
                      totalPages={totalPages}
                      variant="compact"
                    />
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
                  icon={FaRegClipboard}
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
                  <AdminPanelButton
                    title="CATEGORÍAS"
                    description="Crea categorías para clasificar encuestas y elementos del catálogo."
                    icon={TbCategoryPlus}
                    to="/superadmin/categorias"
                    buttonLabel="Ver categorías"
                    theme="teal"
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

              <ul className="catalog-buttons-grid" role="list">
                <li>
                  <PanelButton
                    title="Plantillas"
                    description="Gestiona las plantillas de encuestas."
                    icon={MdOutlineDescription}
                    to="/catalogo/plantillas"
                  />
                </li>

                <li>
                  <PanelButton
                    title="Bloques"
                    description="Gestiona los bloques de preguntas."
                    icon={MdOutlineWidgets}
                    to="/catalogo/bloques"
                  />
                </li>

                <li>
                  <PanelButton
                    title="Preguntas fijas"
                    description="Gestiona las preguntas disponibles en el catálogo."
                    icon={MdOutlineHelpOutline}
                    to="/catalogo/preguntas"
                  />
                </li>
              </ul>
            </section>

            <section
              className="admin-section surveys-summary-section"
              aria-busy={isAdminSurveysLoading}
            >
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">
                  RESUMEN DE ENCUESTAS
                </h2>
              </div>

              {isAdminSurveysLoading ? (
                <Skeleton variant="table" count={ADMIN_SURVEYS_PAGE_SIZE} />
              ) : adminSurveysError ? (
                <p className="panel-error-message" role="alert">{adminSurveysError}</p>
              ) : (
                <Table
                  headers={TABLE_HEADERS}
                  data={tableData}
                  emptyMessage="Todavía no hay encuestas registradas."
                  caption="Resumen de encuestas registradas"
                  footer={adminSurveysTotalItems > 0 ? (
                    <Pagination
                      ariaLabel="Paginación del resumen de encuestas"
                      currentPage={adminSurveysPage}
                      itemLabel="encuestas"
                      onPageChange={setAdminSurveysPage}
                      pageSize={ADMIN_SURVEYS_PAGE_SIZE}
                      totalItems={adminSurveysTotalItems}
                      totalPages={adminSurveysTotalPages}
                    />
                  ) : null}
                />
              )}
            </section>
          </div>
        </section>
      )}

      {panelContent.type === 'EDITOR' && (
        <SurveyOriginModal
          isOpen={isSurveyOriginOpen}
          selectedOrigin={null}
          onClose={() => setIsSurveyOriginOpen(false)}
          onImport={() => {
            setIsSurveyOriginOpen(false);
            navigate('/encuestas/importar');
          }}
          onSelect={(surveyOrigin) => {
            setIsSurveyOriginOpen(false);
            navigate('/encuestas/crear', { state: { surveyOrigin } });
          }}
          returnFocusRef={createSurveyTriggerRef}
        />
      )}
    </main>
  );
}
