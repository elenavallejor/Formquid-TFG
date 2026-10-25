# Frontend de FormQuid

Esta carpeta contiene la interfaz web de FormQuid. Está desarrollada con React 19, React Router 7 y Vite 8. Desde ella se consultan las encuestas, se envían las participaciones y se accede a las herramientas de edición y administración según el rol de cada usuario.

## Recorrido por la aplicación

La página de inicio muestra las encuestas abiertas y listadas de acuerdo con el estado de la sesión. Permite buscar y filtrar por categorías y por modo de acceso. Cada encuesta tiene una dirección formada por su código público. El formulario de participación presenta sus preguntas y comprueba los datos antes de enviarlos al servidor.

Tras iniciar sesión, el panel muestra las acciones disponibles para la cuenta. Un editor puede crear una encuesta vacía, partir de una plantilla del catálogo o importar el documento ODT facilitado por FormQuid. El importador comprueba el archivo, enseña una vista previa y permite continuar con el borrador en el editor. También se puede trabajar con preguntas y bloques reutilizables, revisar la encuesta antes de publicarla, gestionar colaboradores y consultar los resultados individuales y agregados. Si la encuesta contiene preguntas evaluables con peso, la pantalla de resultados muestra además el análisis global ponderado.

Los administradores disponen de pantallas para revisar solicitudes de registro y propuestas para el catálogo, además de supervisar las encuestas. El superadministrador puede gestionar usuarios y categorías. Las rutas protegidas comprueban la sesión y el rol antes de mostrar esas páginas.

## Organización del código

`src/app/router.jsx` define las páginas y sus restricciones de acceso. Las pantallas están en `src/app/routes` y los elementos reutilizables de la interfaz en `src/components`. `src/api` agrupa las peticiones al backend. `src/context/AuthContext.jsx` conserva el estado de autenticación y restaura la sesión al cargar la aplicación. Los recursos visuales se encuentran en `src/assets` y las funciones auxiliares en `src/utils`.

El cliente HTTP utiliza la URL indicada por `VITE_API_BASE_URL`. Si no se configura, apunta a `http://localhost:8080/api/v1`. Las peticiones incluyen las cookies de sesión y el token CSRF en las operaciones que modifican datos. Los errores de la API se convierten en mensajes que pueden mostrar las páginas.

La interfaz incluye opciones de tipografía para las encuestas y componentes que atienden a la navegación por teclado y a los estados de carga. La configuración `vercel.json` contiene una regla de reescritura para que las rutas de React Router puedan abrirse directamente en un alojamiento que utilice ese archivo.

## Desarrollo local

El `Dockerfile` de esta carpeta utiliza Node.js 22. Para trabajar fuera de Docker, se necesita Node.js y npm. Con el backend disponible, ejecutar desde `frontend`:

```bash
npm ci
npm run dev
```

Vite sirve la aplicación en `http://localhost:5173`. Si la API está en otra dirección, `VITE_API_BASE_URL` debe contener la URL completa con `/api/v1`. La variable se lee al iniciar Vite y al generar la compilación.

La plantilla ODT que descarga el importador está en `public/templates`. El comando `npm run generate:survey-template` genera ese archivo y la copia situada en la raíz del repositorio mediante el script de esta carpeta.

## Comprobaciones

Las pruebas de componentes, rutas, utilidades y comunicación con la API utilizan Vitest, React Testing Library y MSW. Se ejecutan desde esta carpeta:

```bash
npm test
npm run test:coverage
npm run lint
npm run build
```

`npm run preview` permite revisar localmente el resultado de la compilación. La configuración del servidor y de la base de datos está explicada en el [README del backend](../backend/README.md). El [README principal](../README.md) recoge el arranque conjunto con Docker Compose.
