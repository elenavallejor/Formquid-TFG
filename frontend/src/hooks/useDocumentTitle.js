import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const PAGE_TITLES = [
  { pattern: /^\/$/, title: 'Inicio' },
  { pattern: /^\/inicia-sesion\/?$/, title: 'Iniciar sesión' },
  { pattern: /^\/solicita-cuenta\/?$/, title: 'Solicitar cuenta' },
  { pattern: /^\/perfil\/?$/, title: 'Mi cuenta' },
  { pattern: /^\/panel\/?$/, title: 'Panel' },
  { pattern: /^\/colaboraciones\/?$/, title: 'Gestión de colaboraciones' },
  { pattern: /^\/sugerir-elemento\/?$/, title: 'Sugerir elemento' },
  { pattern: /^\/encuestas\/crear\/?$/, title: 'Crear encuesta' },
  { pattern: /^\/encuestas\/[^/]+\/editar\/?$/, title: 'Gestionar encuesta' },
  { pattern: /^\/encuestas\/[^/]+\/resultados\/participaciones\/?$/, title: 'Participaciones individuales' },
  { pattern: /^\/encuestas\/[^/]+\/resultados\/?$/, title: 'Resultados de encuesta' },
  { pattern: /^\/encuestas\/?$/, title: 'Encuestas' },
  { pattern: /^\/catalogo\/crear\/pregunta\/?$/, title: 'Crear pregunta' },
  { pattern: /^\/catalogo\/crear\/bloque\/?$/, title: 'Crear bloque' },
  { pattern: /^\/catalogo\/crear\/plantilla\/?$/, title: 'Crear plantilla' },
  { pattern: /^\/catalogo\/editar\/[^/]+\/[^/]+\/?$/, title: 'Editar elemento' },
  { pattern: /^\/catalogo\/detalles\/[^/]+\/[^/]+\/?$/, title: 'Detalles del elemento' },
  { pattern: /^\/catalogo(?:\/[^/]+)?\/?$/, title: 'Catálogo' },
  { pattern: /^\/admin\/encuestas\/?$/, title: 'Supervisión de encuestas' },
  { pattern: /^\/admin\/solicitudes-registro\/?$/, title: 'Solicitudes de registro' },
  { pattern: /^\/admin\/solicitudes-catalogo\/?$/, title: 'Solicitudes del catálogo' },
  { pattern: /^\/superadmin\/usuarios\/?$/, title: 'Gestión de usuarios' },
  { pattern: /^\/[A-HJ-NP-Z2-9]{12}\/?$/i, title: 'Responder encuesta' },
];

function getPageTitle(pathname) {
  return PAGE_TITLES.find(({ pattern }) => pattern.test(pathname))?.title ?? 'Página no encontrada';
}

/** Mantiene un título de documento descriptivo en cada navegación de la SPA. */
export function useDocumentTitle() {
  const { pathname } = useLocation();

  useEffect(() => {
    document.title = `${getPageTitle(pathname)} | FormQuid`;
  }, [pathname]);
}
