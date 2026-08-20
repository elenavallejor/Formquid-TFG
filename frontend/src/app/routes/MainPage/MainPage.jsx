import './MainPage.css';

/**
 * Página principal de FormQuid.
 *
 * Esta pantalla representa la ruta inicial de la aplicación.
 * Por ahora se mantiene vacía porque primero se va a construir la estructura
 * común del frontend: cabecera, pie de página y posteriormente la pantalla
 * de inicio de sesión.
 */
export function MainPage() {
  return (
    <main className="main-page" aria-labelledby="main-page-title">
      <h1 id="main-page-title" className="sr-only">
        Página principal de FormQuid
      </h1>
    </main>
  );
}