import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { useAuth } from '../../../context/AuthContext.jsx';

import './PanelPage.css';

const ROLE_CONTENT = {
  EDITOR: {
    title: 'Panel de editor',
    description: 'Gestiona tus encuestas, colaboraciones y resultados',
    type: 'EDITOR',
    actions: [
      {
        id: 'crear-encuesta',
        label: 'Crear encuesta',
        description: 'Diseña una nueva encuesta desde cero.',
        to: '/encuestas/nueva',
        icon: 'plus',
      },
      {
        id: 'mis-colaboraciones',
        label: 'Mis colaboraciones',
        description: 'Revisa invitaciones y encuestas compartidas contigo.',
        to: '/colaboraciones',
        icon: 'users',
      },
      {
        id: 'resultados',
        label: 'Resultados',
        description: 'Consulta y analiza los resultados de tus encuestas.',
        to: '/resultados',
        icon: 'chart',
      },
      {
        id: 'sugerir-elemento',
        label: 'Sugerir elemento',
        description: 'Propón nuevas funciones o mejoras para FormQuid.',
        to: '/sugerencias',
        icon: 'lightbulb',
      },
    ],
  },

  ADMINISTRADOR: {
    title: 'PANEL DE ADMINISTRADOR',
    description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.',
    type: 'ADMIN_SUPER',
    sections: [
      {
        title: 'PENDIENTES DE GESTIÓN',
        items: [
          {
            id: 'solicitudes-catalogo',
            title: 'SOLICITUDES PARA CATÁLOGO',
            description: 'Revisa y gestiona las propuestas de nuevos elementos enviadas por los editores.',
            badgeCount: 3,
            badgeText: 'pendientes',
            buttonLabel: 'Ver solicitudes',
            to: '/admin/solicitudes-catalogo',
            colorTheme: 'teal',
            icon: 'lightbulb',
          },
          {
            id: 'solicitudes-registro',
            title: 'SOLICITUDES DE REGISTRO',
            description: 'Revisa las solicitudes de alta de nuevos editores.',
            badgeCount: 15,
            badgeText: 'pendientes',
            buttonLabel: 'Ver registros',
            to: '/admin/solicitudes-registro',
            colorTheme: 'orange',
            icon: 'user-plus',
          },
        ],
      },
      {
        title: 'OTRAS GESTIONES (SOLO SUPERADMINISTRADOR)',
        items: [
          {
            id: 'usuarios-permisos',
            title: 'USUARIOS Y PERMISOS',
            description: 'Gestiona usuarios y permisos en la plataforma.',
            buttonLabel: 'Ver registros',
            to: '/superadmin/usuarios',
            colorTheme: 'purple',
            icon: 'shield',
          },
        ],
      },
    ],
    hasSurveySummary: true,
  },

  SUPERADMINISTRADOR: {
    title: 'PANEL DE SUPERADMINISTRADOR',
    description: 'Desde aquí puedes gestionar todas las áreas de la plataforma.',
    type: 'ADMIN_SUPER',
    sections: [
      {
        title: 'PENDIENTES DE GESTIÓN',
        items: [
          {
            id: 'solicitudes-catalogo',
            title: 'SOLICITUDES PARA CATÁLOGO',
            description: 'Revisa y gestiona las propuestas de nuevos elementos enviadas por los editores.',
            badgeCount: 3,
            badgeText: 'pendientes',
            buttonLabel: 'Ver solicitudes',
            to: '/admin/solicitudes-catalogo',
            colorTheme: 'teal',
            icon: 'lightbulb',
          },
          {
            id: 'solicitudes-registro',
            title: 'SOLICITUDES DE REGISTRO',
            description: 'Revisa las solicitudes de alta de nuevos editores.',
            badgeCount: 15,
            badgeText: 'pendientes',
            buttonLabel: 'Ver registros',
            to: '/admin/solicitudes-registro',
            colorTheme: 'orange',
            icon: 'user-plus',
          },
        ],
      },
      {
        title: 'OTRAS GESTIONES (SOLO SUPERADMINISTRADOR)',
        items: [
          {
            id: 'usuarios-permisos',
            title: 'USUARIOS Y PERMISOS',
            description: 'Gestiona usuarios y permisos en la plataforma.',
            buttonLabel: 'Ver registros',
            to: '/superadmin/usuarios',
            colorTheme: 'purple',
            icon: 'shield',
          },
        ],
      },
    ],
    hasSurveySummary: true,
  },
};

// Se mantienen los nombres exactos de los roles sin normalización
function getRoleContent(role) {
  return ROLE_CONTENT[role] ?? null;
}

export function PanelPage() {
  const navigate = useNavigate();
  const { restoreSession, user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);

  // Estado mock de encuestas para el grid/tabla
  const [mockSurveys] = useState([
    {
      id: 1,
      title: 'Satisfacción del curso 2024',
      status: 'Borrador',
      tags: ['Educación', 'Satisfacción'],
      editor: 'Elena Vallejo',
      responses: 0,
      updatedAt: '2026-08-10',
    },
    {
      id: 2,
      title: 'Evaluación de servicios 2024',
      status: 'Abierta',
      tags: ['Servicios', 'Experiencia'],
      editor: 'Carlos Ruiz',
      responses: 45,
      updatedAt: '2026-08-15',
    },
    {
      id: 3,
      title: 'Consulta ciudadana',
      status: 'Cerrada',
      tags: ['Participación', 'Ciudadanía'],
      editor: 'Ana Gómez',
      responses: 128,
      updatedAt: '2026-08-01',
    },
    {
      id: 4,
      title: 'Clima laboral interno',
      status: 'Abierta',
      tags: ['Recursos Humanos', 'Clima laboral'],
      editor: 'Elena Vallejo',
      responses: 32,
      updatedAt: '2026-08-18',
    },
  ]);

  useEffect(() => {
    let isMounted = true;

    async function checkSession() {
      const currentUser = await restoreSession();

      if (!isMounted) return;

      if (!currentUser) {
        navigate('/inicia-sesion', { replace: true });
        return;
      }

      setIsLoading(false);
    }

    void checkSession();

    return () => {
      isMounted = false;
    };
  }, [navigate, restoreSession]);

  const panelContent = useMemo(
    () => getRoleContent(user?.role),
    [user?.role]
  );

  if (isLoading) {
    return (
      <main className="panel-page" aria-busy="true">
        <p className="panel-status">Cargando panel...</p>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  if (!panelContent) {
    return (
      <main className="panel-page">
        <section className="panel-container" aria-labelledby="panel-title">
          <h1 id="panel-title">Rol de usuario no reconocido</h1>
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
        {/* Cabecera Principal */}
        <div className="panel-header">
          <h1 id="panel-title" className="panel-title">
            {panelContent.title}
          </h1>
          <p className="panel-description">{panelContent.description}</p>
        </div>

        {/* Layout para EDITOR */}
        {panelContent.type === 'EDITOR' && (
          <>
            <div className="panel-actions-grid" aria-label="Acciones disponibles">
              {panelContent.actions.map((action) => (
                <Link key={action.id} className="panel-card-action" to={action.to}>
                  <div className={`panel-card-icon icon-${action.icon}`}>
                    <span className="icon-placeholder" />
                  </div>
                  <div className="panel-card-content">
                    <h3 className="panel-card-title">{action.label}</h3>
                    <p className="panel-card-description">{action.description}</p>
                  </div>
                  <span className="panel-card-arrow">&rsaquo;</span>
                </Link>
              ))}
            </div>

            {/* Sección de Encuestas para Editor */}
            <section className="panel-section surveys-section">
              <div className="surveys-section-header">
                <h2>Mis encuestas</h2>
                <Link to="/encuestas" className="btn-secondary">
                  Ver todas
                </Link>
              </div>

              <div className="surveys-filters">
                <div className="search-input-wrapper">
                  <input type="text" placeholder="Buscar encuestas..." />
                </div>
                <select defaultValue="todos">
                  <option value="todos">Todos los estados</option>
                  <option value="borrador">Borrador</option>
                  <option value="abierta">Abierta</option>
                  <option value="cerrada">Cerrada</option>
                </select>
                <select defaultValue="recientes">
                  <option value="recientes">Más recientes primero</option>
                  <option value="antiguas">Más antiguas primero</option>
                </select>
              </div>

              <div className="surveys-cards-grid">
                {mockSurveys.map((survey) => (
                  <article key={survey.id} className="survey-card">
                    <div className="survey-card-image">
                      <div className="image-placeholder" />
                    </div>
                    <div className="survey-card-body">
                      <h3>{survey.title}</h3>
                      <span className={`badge badge-${survey.status.toLowerCase()}`}>
                        {survey.status}
                      </span>
                      <div className="survey-card-tags">
                        {survey.tags.map((tag) => (
                          <span key={tag} className="tag">
                            {tag}
                          </span>
                        ))}
                      </div>
                      <div className="survey-card-actions">
                        <Link to={`/encuestas/${survey.id}/editar`} className="btn-action edit">
                          Editar
                        </Link>
                        <Link to={`/encuestas/${survey.id}`} className="btn-action view">
                          Ver
                        </Link>
                        <button type="button" className="btn-options" aria-label="Opciones">
                          &vellip;
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </>
        )}

        {/* Layout para ADMINISTRADOR / SUPERADMINISTRADOR */}
        {panelContent.type === 'ADMIN_SUPER' && (
          <div className="admin-panel-content">
            {panelContent.sections.map((section) => (
              <div key={section.title} className="admin-section">
                <h2 className="admin-section-title">{section.title}</h2>
                <div className="admin-cards-grid">
                  {section.items.map((item) => (
                    <div
                      key={item.id}
                      className={`admin-management-card theme-${item.colorTheme}`}
                    >
                      <div className="admin-card-header">
                        <div className="admin-card-icon">
                          <span className="icon-placeholder" />
                        </div>
                        <div className="admin-card-info">
                          <h3>{item.title}</h3>
                          <p>{item.description}</p>
                        </div>
                      </div>

                      <div className="admin-card-footer">
                        {item.badgeCount !== undefined ? (
                          <div className="admin-badge">
                            <span className="badge-number">{item.badgeCount}</span>
                            <span className="badge-text">{item.badgeText}</span>
                          </div>
                        ) : (
                          <div className="admin-badge-spacer" />
                        )}

                        <Link to={item.to} className={`btn-admin-action theme-${item.colorTheme}`}>
                          {item.buttonLabel} &rarr;
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}

            {/* Resumen de encuestas para paneles de administración */}
            {panelContent.hasSurveySummary && (
              <section className="admin-section surveys-summary-section">
                <div className="admin-section-header-row">
                  <h2 className="admin-section-title">RESUMEN DE ENCUESTAS</h2>
                  <Link to="/admin/encuestas" className="btn-admin-action theme-teal">
                    Ver encuestas &rarr;
                  </Link>
                </div>

                <div className="admin-table-wrapper">
                  <table className="admin-surveys-table">
                    <thead>
                      <tr>
                        <th>Título</th>
                        <th>Editor</th>
                        <th>Estado</th>
                        <th>Respuestas</th>
                        <th>Actualización</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mockSurveys.map((survey) => (
                        <tr key={survey.id}>
                          <td>{survey.title}</td>
                          <td>{survey.editor}</td>
                          <td>
                            <span className={`badge badge-${survey.status.toLowerCase()}`}>
                              {survey.status}
                            </span>
                          </td>
                          <td>{survey.responses}</td>
                          <td>{survey.updatedAt}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </div>
        )}
      </section>
    </main>
  );
}