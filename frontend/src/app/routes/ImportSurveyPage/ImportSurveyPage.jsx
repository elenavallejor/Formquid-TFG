import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  MdCheckCircle,
  MdClose,
  MdOutlineFileDownload,
  MdOutlineInsertDriveFile,
  MdUploadFile,
} from 'react-icons/md';

import { previewSurveyImport } from '../../../api/surveysApi.js';
import { BackButton } from '../../../components/BackButton/BackButton.jsx';
import { Breadcrumbs } from '../../../components/Breadcrumbs/Breadcrumbs.jsx';

import './ImportSurveyPage.css';

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ODT_MIME_TYPE = 'application/vnd.oasis.opendocument.text';
const TEMPLATE_URL = `${import.meta.env.BASE_URL}templates/plantilla-importacion-encuestas-formquid.odt`;

function formatFileSize(size) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export function ImportSurveyPage() {
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const [isValidating, setIsValidating] = useState(false);

  function selectFile(nextFile) {
    setPreview(null);
    setErrorMessage('');
    if (!nextFile) {
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      return;
    }
    if (!nextFile.name.toLocaleLowerCase('es').endsWith('.odt')) {
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      setErrorMessage('Selecciona un documento con formato .odt.');
      return;
    }
    if (nextFile.size > MAX_FILE_SIZE) {
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      setErrorMessage('El documento no puede superar 5 MB.');
      return;
    }
    setFile(nextFile);
  }

  function clearFile() {
    setFile(null);
    setPreview(null);
    setErrorMessage('');
    if (inputRef.current) inputRef.current.value = '';
  }

  async function validateDocument() {
    if (!file || isValidating) return;
    setIsValidating(true);
    setErrorMessage('');
    setPreview(null);
    try {
      setPreview(await previewSurveyImport(file));
    } catch (error) {
      setErrorMessage(error.message || 'No se ha podido validar el documento.');
    } finally {
      setIsValidating(false);
    }
  }

  function continueWithImport() {
    if (!preview || !file) return;
    navigate('/encuestas/crear', {
      state: {
        importedSurvey: {
          ...preview,
          fileName: file.name,
        },
      },
    });
  }

  return (
    <main className="import-survey-page">
      <div className="import-survey-container">
        <Breadcrumbs items={[
          { label: 'PANEL', to: '/panel' },
          { label: 'CREAR ENCUESTA', to: '/encuestas/crear' },
          { label: 'IMPORTAR DESDE ODT' },
        ]} />

        <BackButton fallbackTo="/encuestas/crear" className="import-survey-back" />

        <header className="import-survey-header">
          <div>
            <h1>IMPORTAR ENCUESTA DESDE ODT</h1>
            <p>Utiliza la plantilla de FormQuid para generar un borrador que podrás revisar y editar.</p>
          </div>
        </header>

        <section className="import-survey-card" aria-labelledby="download-template-title">
          <div className="import-survey-step-number" aria-hidden="true">1</div>
          <div className="import-survey-step-content">
            <h2 id="download-template-title">DESCARGA LA PLANTILLA</h2>
            <p>Completa la plantilla vacía situada al final del documento. El ejemplo anterior es solo una referencia y no se importará.</p>
            <a className="import-survey-download" href={TEMPLATE_URL} download="Plantilla importacion de encuestas FormQuid.odt">
              <MdOutlineFileDownload aria-hidden="true" />
              <span>Descargar plantilla ODT</span>
              <small>.odt</small>
            </a>
          </div>
        </section>

        <section className="import-survey-card" aria-labelledby="upload-document-title">
          <div className="import-survey-step-number" aria-hidden="true">2</div>
          <div className="import-survey-step-content">
            <h2 id="upload-document-title">SELECCIONA EL DOCUMENTO</h2>
            <p>Sube la plantilla completada para comprobar su estructura antes de crear la encuesta.</p>

            <button
              type="button"
              className={`import-survey-dropzone ${isDragging ? 'is-dragging' : ''} ${errorMessage ? 'has-error' : ''}`}
              aria-describedby={`survey-odt-file-help${errorMessage ? ' survey-odt-file-error' : ''}`}
              aria-invalid={Boolean(errorMessage)}
              onClick={() => inputRef.current?.click()}
              onDragEnter={(event) => { event.preventDefault(); setIsDragging(true); }}
              onDragOver={(event) => event.preventDefault()}
              onDragLeave={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget)) setIsDragging(false);
              }}
              onDrop={(event) => {
                event.preventDefault();
                setIsDragging(false);
                selectFile(event.dataTransfer.files?.[0]);
              }}
            >
              <MdUploadFile aria-hidden="true" />
              <strong>Arrastra aquí el documento</strong>
              <span>o</span>
              <span className="import-survey-file-button">Seleccionar archivo</span>
              <small id="survey-odt-file-help">Formato admitido: .odt · Tamaño máximo: 5 MB</small>
            </button>
            <input
              ref={inputRef}
              id="survey-odt-file"
              className="import-survey-file-input"
              type="file"
              tabIndex={-1}
              accept={`.odt,${ODT_MIME_TYPE}`}
              onChange={(event) => selectFile(event.target.files?.[0])}
            />

            {file && (
              <div className="import-survey-selected-file">
                <MdOutlineInsertDriveFile aria-hidden="true" />
                <span><strong>{file.name}</strong><small>{formatFileSize(file.size)}</small></span>
                <button type="button" aria-label={`Quitar ${file.name}`} onClick={clearFile}>
                  <MdClose aria-hidden="true" />
                </button>
              </div>
            )}

            {errorMessage && <p id="survey-odt-file-error" className="import-survey-error" role="alert">{errorMessage}</p>}

            <div className="import-survey-actions">
              <button
                type="button"
                className="import-survey-validate"
                disabled={!file || isValidating}
                aria-busy={isValidating}
                onClick={validateDocument}
              >
                {isValidating ? 'Validando documento…' : 'Validar documento'}
              </button>
            </div>
          </div>
        </section>

        {preview && (
          <section className="import-survey-card import-survey-result" aria-labelledby="import-result-title" aria-live="polite">
            <MdCheckCircle className="import-survey-result-icon" aria-hidden="true" />
            <div className="import-survey-step-content">
              <h2 id="import-result-title">DOCUMENTO VÁLIDO</h2>
              <p className="import-survey-result-title">{preview.title}</p>
              <p>{preview.blockCount} {preview.blockCount === 1 ? 'bloque' : 'bloques'} · {preview.questionCount} {preview.questionCount === 1 ? 'pregunta' : 'preguntas'}</p>
              <div className="import-survey-actions">
                <button type="button" className="import-survey-replace" onClick={clearFile}>Reemplazar archivo</button>
                <button type="button" className="import-survey-continue" onClick={continueWithImport}>Continuar con esta encuesta</button>
              </div>
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
