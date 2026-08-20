import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { useAuth } from '../../../context/AuthContext.jsx';

import './PanelPage.css';

const ROLE_CONTENT = {
  EDITOR: {
    title: 'Panel de editor',
    description:
      'Gestiona tus encuestas, revisa colaboraciones y consulta resultados.',
    actions: [
      {
        label: 'Crear encuesta',
        description: 'Diseña una nueva encuesta desde cero o a partir de una plantilla.',
        to: '/encuestas/nueva',
      },
      {
        label: 'Mis encuestas',
        description: 'Consulta y edita las encuestas que has creado o compartes.',
        to: '/encuestas',
      },
      {
        label: 'Colaboraciones',
        description: 'Revisa las encuestas en las que participas como colaborador.',
        to: '/colaboraciones',
      },
      {
        label: 'Resultados',
        description: 'Consulta estadísticas de encuestas cerradas o publicadas.',
        to: '/resultados',
      },
    ],
  },

  ADMIN: {
    title: 'Panel de administrador',
    description:
      'Gestiona solicitudes, catálogo y supervisa las encuestas de la plataforma.',
    actions: [
      {
        label: 'Solicitudes de registro',
        description: 'Aprueba o rechaza nuevas solicitudes de editores.',
        to: '/admin/solicitudes-registro',
      },
      {
        label: 'Solicitudes de catálogo',
        description: 'Revisa propuestas de preguntas, bloques y plantillas.',
        to: '/admin/solicitudes-catalogo',
      },
      {
        label: 'Encuestas',
        description: 'Supervisa las encuestas creadas dentro de la plataforma.',
        to: '/admin/encuestas',
      },
    ],
  },

  SUPERADMIN: {
    title: 'Panel de superadministrador',
    description:
      'Administra usuarios, roles y operaciones globales del sistema.',
    actions: [
      {
        label: 'Gestión de usuarios',
        description: 'Consulta usuarios registrados y administra sus permisos.',
        to: '/superadmin/usuarios',
      },
      {
        label: 'Gestión de roles',
        description: 'Concede o revoca permisos de administración.',
        to: '/superadmin/roles',
      },
      {
        label: 'Supervisión global',
        description: 'Accede a las herramientas generales de control del sistema.',
        to: '/superadmin/supervision',
      },
    ],
  },
};

function normalizeRole(role) {
  if (role === 'ADMINISTRADOR') {
    return 'ADMIN';
  }

  if (role === 'SUPER_ADMIN' || role === 'SUPERADMINISTRADOR') {
    return 'SUPERADMIN';
  }

  return role;
}

function getUserDisplayName(user) {
  return user?.name || user?.fullName || user?.email || 'Usuario';
}

function getRoleContent(role) {
  return ROLE_CONTENT[normalizeRole(role)] ?? null;
}

export function PanelPage() {
  const navigate = useNavigate();

  const {
    restoreSession,
    user,
  } = useAuth();

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function checkSession() {
      const currentUser = await restoreSession();

      if (!isMounted) {
        return;
      }

      if (!currentUser) {
        navigate('/inicia-sesion', {
          replace: true,
        });

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
            Tu cuenta existe, pero el rol asociado no tiene un panel configurado.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="panel-page">
      <section className="panel-container" aria-labelledby="panel-title">
        <div className="panel-header">
          <p className="panel-eyebrow">Área privada</p>

          <h1 id="panel-title">
            {panelContent.title}
          </h1>

          <p className="panel-description">
            Hola, {getUserDisplayName(user)}. {panelContent.description}
          </p>
        </div>

        <div className="panel-actions" aria-label="Acciones disponibles">
          {panelContent.actions.map((action) => (
            <Link
              className="panel-action-card"
              key={action.to}
              to={action.to}
            >
              <span className="panel-action-title">
                {action.label}
              </span>

              <span className="panel-action-description">
                {action.description}
              </span>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}