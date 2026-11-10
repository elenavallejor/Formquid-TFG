import './ManualPage.css';

const MANUAL_PDF = '/manual-de-usuario.pdf';

export function ManualPage() {
  return (
    <section className="manual-page" aria-label="Manual de usuario">
      <object
        className="manual-page-document"
        data={MANUAL_PDF}
        type="application/pdf"
        title="Manual de usuario de FormQuid"
      >
        <p>
          Tu navegador no puede mostrar el PDF.
          {' '}
          <a href={MANUAL_PDF}>Descargar manual de usuario</a>
        </p>
      </object>
    </section>
  );
}
