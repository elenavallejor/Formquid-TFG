import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdOutlineLightbulb, MdOutlinePersonAdd, MdOutlineShield, MdOutlinePoll, MdOutlineHelpOutline, MdOutlineWidgets, MdOutlineDescription, MdAdd, MdPeopleOutline, MdOutlineBarChart, MdSearch, MdChevronLeft, MdChevronRight } from 'react-icons/md';
import { useAuth } from '../../../context/AuthContext.jsx';
import { AdminPanelButton } from '../../../components/AdminPanelButton/AdminPanelButton.jsx';
import { PanelButton } from '../../../components/PanelButton/PanelButton.jsx';
import { Table } from '../../../components/Table/Table.jsx';
import { SurveyCard } from '../../../components/SurveyCard/SurveyCard.jsx';
import { getPendingRegistrationRequests } from '../../../api/registrationRequestsApi.js';
import { getPendingElementRequests } from '../../../api/elementRequestsApi.js';
import { getAdminSurveysSummary, getMySurveys } from '../../../api/surveysApi.js';
import './PanelPage.css';

const ROLE_CONTENT = {
  EDITOR: { title: 'PANEL DE EDITOR', description: 'Desde aquí puedes crear y gestionar elementos y encuestas.', type: 'EDITOR' },
  ADMINISTRADOR: { title: 'PANEL DE ADMINISTRADOR', description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.', type: 'ADMIN_SUPER' },
  SUPERADMIN: { title: 'PANEL DE SUPERADMINISTRADOR', description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.', type: 'ADMIN_SUPER' },
};

const EDITOR_ACTIONS = [
  { title: 'Crear encuesta', description: 'Crea una nueva encuesta desde cero o a partir de una plantilla.', icon: MdAdd, to: '/encuestas/crear', buttonText: 'Crear encuesta' },
  { title: 'Colaboraciones', description: 'Gestiona colaboradores, solicitudes y permisos de tus encuestas.', icon: MdPeopleOutline, to: '/colaboraciones', buttonText: 'Ver colaboraciones' },
  { title: 'Resultados', description: 'Consulta y analiza los resultados de tus encuestas.', icon: MdOutlineBarChart, to: '/resultados', buttonText: 'Ver resultados' },
  { title: 'Sugerir elemento', description: 'Propón elementos que puedan ser útiles para otros editores.', icon: MdOutlineLightbulb, to: '/sugerir-elemento', buttonText: 'Sugerir elemento' },
];

const LIFECYCLE_LABELS = { DRAFT: 'Borrador', OPEN: 'Abierta', CLOSED: 'Cerrada' };
const ACCESS_MODE_LABELS = { PUBLIC: 'Pública', PASSWORD_PROTECTED: 'Con contraseña', AUTHENTICATED: 'Usuarios autenticados' };
const VISIBILITY_LABELS = { LISTED: 'Listada', UNLISTED: 'No listada' };

export function PanelPage() {
  const navigate = useNavigate();
  const { restoreSession, user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [dataError, setDataError] = useState(null);

  const [catalogRequestsCount, setCatalogRequestsCount] = useState(0);
  const [registrationRequestsCount, setRegistrationRequestsCount] = useState(0);
  const [adminSurveys, setAdminSurveys] = useState([]);

  const [editorSurveys, setEditorSurveys] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 8;

  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('RECENT');

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

        if (role === 'EDITOR') {
          const res = await getMySurveys();
          if (isMounted) {
            const surveyList = Array.isArray(res) ? res : res?.content || res?.data || [];
            setEditorSurveys(surveyList);
          }
        } else if (role === 'ADMINISTRADOR' || role === 'SUPERADMIN') {
          const [catalogRes, registrationRes, surveysRes] = await Promise.allSettled([
            getPendingElementRequests(),
            getPendingRegistrationRequests(),
            getAdminSurveysSummary(),
          ]);

          if (isMounted) {
            if (catalogRes.status === 'fulfilled') {
              const res = catalogRes.value;
              setCatalogRequestsCount(Array.isArray(res) ? res.length : res?.content?.length || 0);
            }
            if (registrationRes.status === 'fulfilled') {
              const res = registrationRes.value;
              setRegistrationRequestsCount(Array.isArray(res) ? res.length : res?.content?.length || 0);
            }
            if (surveysRes.status === 'fulfilled') {
              const res = surveysRes.value;
              setAdminSurveys(Array.isArray(res) ? res : res?.content || res?.data || []);
            }
          }
        }
      } catch (error) {
        if (isMounted) {
          console.error('Error al cargar datos del panel:', error);
          setDataError('No se han podido obtener algunos datos del panel.');
        }
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    void initializePanel();

    return () => { isMounted = false; };
  }, [navigate, restoreSession]);

  const panelContent = useMemo(() => ROLE_CONTENT[user?.role] ?? null, [user?.role]);
  const isSuperAdmin = user?.role === 'SUPERADMIN';

  const filteredEditorSurveys = useMemo(() => {
    return editorSurveys
      .filter((survey) => {
        const matchSearch = survey.title?.toLowerCase().includes(searchQuery.toLowerCase());
        const status = survey.lifecycleStatus || 'DRAFT';
        const matchStatus = statusFilter === 'ALL' || status === statusFilter;
        return matchSearch && matchStatus;
      })
      .sort((a, b) => {
        const dateA = new Date(a.updatedAt || a.createdAt || 0).getTime();
        const dateB = new Date(b.updatedAt || b.createdAt || 0).getTime();
        return sortOrder === 'RECENT' ? dateB - dateA : dateA - dateB;
      });
  }, [editorSurveys, searchQuery, statusFilter, sortOrder]);

  const totalPages = Math.ceil(filteredEditorSurveys.length / pageSize) || 1;
  const paginatedEditorSurveys = useMemo(() => {
    const start = currentPage * pageSize;
    return filteredEditorSurveys.slice(start, start + pageSize);
  }, [filteredEditorSurveys, currentPage, pageSize]);

  const tableHeaders = ['Título', 'Editor', 'Ciclo de vida', 'Modo de acceso', 'Visibilidad', 'Respuestas', 'Actualización'];

  const tableData = useMemo(() => {
    return adminSurveys.slice(0, 5).map((survey, index) => {
      const id = survey.id ?? index;
      const lifecycle = survey.lifecycleStatus || survey.lifecycle || 'DRAFT';
      const accessMode = survey.accessMode ?? 'PUBLIC';
      const visibility = survey.visibility ?? 'LISTED';
      const formattedDate = survey.updatedAt ? new Date(survey.updatedAt).toLocaleDateString() : '-';

      return [
        survey.title ?? 'Sin título',
        survey.editorName || 'Sistema',
        <span key={`lifecycle-${id}`} className={`table-badge table-badge-${lifecycle.toLowerCase()}`}>{LIFECYCLE_LABELS[lifecycle] ?? lifecycle}</span>,
        <span key={`access-${id}`} className={`table-badge table-badge-${accessMode.toLowerCase()}`}>{ACCESS_MODE_LABELS[accessMode] ?? accessMode}</span>,
        <span key={`visibility-${id}`} className={`table-badge table-badge-${visibility.toLowerCase()}`}>{VISIBILITY_LABELS[visibility] ?? visibility}</span>,
        survey.responses ?? 0,
        formattedDate,
      ];
    });
  }, [adminSurveys]);

  if (isLoading) return <main className="panel-page" aria-busy="true"><p className="panel-status">Cargando datos del panel...</p></main>;
  if (!user || !panelContent) return null;

  return (
    <main className={`panel-page panel-type-${panelContent.type.toLowerCase()}`}>
      {panelContent.type === 'EDITOR' && (
        <div className="editor-panel-wrapper">
          <section className="editor-top-section">
            <div className="panel-container">
              <div className="panel-header">
                <h1 className="panel-title">{panelContent.title}</h1>
                <p className="panel-description">{panelContent.description}</p>
                {dataError && <p className="panel-error-message">{dataError}</p>}
              </div>

              <div className="editor-actions-grid">
                {EDITOR_ACTIONS.map((card, idx) => {
                  const Icon = card.icon;
                  return (
                    <article key={idx} className="editor-action-card">
                      <div className="action-card-icon-circle"><Icon aria-hidden="true" /></div>
                      <h2 className="action-card-title">{card.title}</h2>
                      <p className="action-card-description">{card.description}</p>
                      <button type="button" className="action-card-btn" onClick={() => navigate(card.to)}>{card.buttonText}</button>
                    </article>
                  );
                })}
              </div>

              {/* Sección Acceso al Catálogo en la vista del Editor */}
              <div className="admin-section editor-catalog-section">
                <div className="admin-section-header-row">
                  <h2 className="admin-section-title">ACCESO AL CATÁLOGO</h2>
                  <Link to="/catalogo" className="btn-admin-action-link">Ver catálogo &rarr;</Link>
                </div>

                <div className="catalog-buttons-grid">
                  <PanelButton
                    title="Preguntas fijas"
                    description="Consulta las preguntas disponibles en el catálogo."
                    icon={MdOutlineHelpOutline}
                    to="/catalogo/preguntas"
                  />

                  <PanelButton
                    title="Bloques"
                    description="Consulta los bloques de preguntas reutilizables."
                    icon={MdOutlineWidgets}
                    to="/catalogo/bloques"
                  />

                  <PanelButton
                    title="Plantillas"
                    description="Consulta las plantillas de encuesta."
                    icon={MdOutlineDescription}
                    to="/catalogo/plantillas"
                  />
                </div>
              </div>
            </div>
          </section>

          <section className="editor-surveys-section">
            <div className="panel-container">
              <div className="surveys-header-row">
                <div>
                  <h2 className="surveys-section-title">Mis encuestas</h2>
                  <p className="surveys-section-subtitle">Encuestas en las que has participado o creado.</p>
                </div>
                <Link to="/admin/encuestas" className="btn-see-all">Ver todas</Link>
              </div>

              <div className="surveys-filters-bar">
                <div className="surveys-search-wrapper">
                  <MdSearch className="search-icon" aria-hidden="true" />
                  <input type="text" placeholder="Buscar encuestas..." value={searchQuery} onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(0); }} />
                </div>

                <div className="surveys-selects-group">
                  <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(0); }}>
                    <option value="ALL">Todos los estados</option>
                    <option value="OPEN">Abierta</option>
                    <option value="DRAFT">Borrador</option>
                    <option value="CLOSED">Cerrada</option>
                  </select>

                  <select value={sortOrder} onChange={(e) => { setSortOrder(e.target.value); setCurrentPage(0); }}>
                    <option value="RECENT">Más recientes primero</option>
                    <option value="OLDEST">Más antiguas primero</option>
                  </select>
                </div>
              </div>

              <div className="surveys-cards-grid">
                {paginatedEditorSurveys.length === 0 ? (
                  <p className="panel-status">No se encontraron encuestas.</p>
                ) : (
                  paginatedEditorSurveys.map((survey) => (
                    <SurveyCard key={survey.id} survey={survey} onEdit={(id) => navigate(`/encuestas/${id}/editar`)} onView={(id) => navigate(`/encuestas/${id}`)} onMoreOptions={(id) => console.log('Opciones', id)} />
                  ))
                )}
              </div>

              {totalPages > 1 && (
                <div className="pagination-controls">
                  <button type="button" className="btn-pagination" disabled={currentPage === 0} onClick={() => setCurrentPage((prev) => prev - 1)}>
                    <MdChevronLeft aria-hidden="true" />
                    <span>Anterior</span>
                  </button>

                  <span className="pagination-info">Página <strong>{currentPage + 1}</strong> de <strong>{totalPages}</strong></span>

                  <button type="button" className="btn-pagination" disabled={currentPage >= totalPages - 1} onClick={() => setCurrentPage((prev) => prev + 1)}>
                    <span>Siguiente</span>
                    <MdChevronRight aria-hidden="true" />
                  </button>
                </div>
              )}
            </div>
          </section>
        </div>
      )}

      {panelContent.type === 'ADMIN_SUPER' && (
        <section className="panel-container" aria-labelledby="panel-title">
          <div className="panel-header">
            <h1 id="panel-title" className="panel-title">{panelContent.title}</h1>
            <p className="panel-description">{panelContent.description}</p>
            {dataError && <p className="panel-error-message">{dataError}</p>}
          </div>

          <div className="admin-panel-content">
            <section className="admin-section">
              <h2 className="admin-section-title">PENDIENTES DE GESTIÓN</h2>
              <div className="admin-cards-grid-3">
                <AdminPanelButton title="SOLICITUDES PARA CATÁLOGO" description="Revisa y gestiona las propuestas de nuevos elementos enviadas por los editores." elementName="solicitudes" count={catalogRequestsCount} icon={MdOutlineLightbulb} to="/admin/solicitudes-catalogo" theme="teal" counterLabel="pendientes" />
                <AdminPanelButton title="ENCUESTAS CREADAS" description="Gestiona y supervisa todas las encuestas registradas en la plataforma." elementName="encuestas" count={adminSurveys.length} icon={MdOutlinePoll} to="/admin/encuestas" theme="teal" counterLabel="creadas" />
                <AdminPanelButton title="SOLICITUDES DE REGISTRO" description="Revisa las solicitudes de alta de nuevos editores." elementName="registros" count={registrationRequestsCount} icon={MdOutlinePersonAdd} to="/admin/solicitudes-registro" theme="teal" counterLabel="pendientes" />
              </div>
            </section>

            {isSuperAdmin && (
              <section className="admin-section">
                <h2 className="admin-section-title">OTRAS GESTIONES (SOLO SUPERADMINISTRADOR)</h2>
                <div className="admin-cards-grid-full">
                  <AdminPanelButton title="USUARIOS Y PERMISOS" description="Gestiona usuarios y permisos en la plataforma." icon={MdOutlineShield} to="/superadmin/usuarios" buttonLabel="Ver registros" theme="purple" fullWidth={true} />
                </div>
              </section>
            )}

            <section className="admin-section">
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">ACCESO AL CATÁLOGO</h2>
                <Link to="/catalogo" className="btn-admin-action-link">Ver catálogo &rarr;</Link>
              </div>

              <div className="catalog-buttons-grid">
                <PanelButton title="Preguntas fijas" description="Gestiona las preguntas disponibles en el catálogo." icon={MdOutlineHelpOutline} to="/catalogo/preguntas" />
                <PanelButton title="Bloques" description="Gestiona los bloques de preguntas reutilizables." icon={MdOutlineWidgets} to="/catalogo/bloques" />
                <PanelButton title="Plantillas" description="Gestiona las plantillas de encuesta." icon={MdOutlineDescription} to="/catalogo/plantillas" />
              </div>
            </section>

            <section className="admin-section surveys-summary-section">
              <div className="admin-section-header-row">
                <h2 className="admin-section-title">RESUMEN DE ENCUESTAS</h2>
                <Link to="/admin/encuestas" className="btn-admin-action-link">Ver encuestas &rarr;</Link>
              </div>

              <Table headers={tableHeaders} data={tableData} />
            </section>
          </div>
        </section>
      )}
    </main>
  );
}