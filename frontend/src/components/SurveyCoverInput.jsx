import { useEffect, useRef } from 'react';
import { MdClose, MdImage, MdUploadFile } from 'react-icons/md';

import { resolveSurveyImageUrl } from '../api/apiClient.js';

const MAX_FILE_SIZE = 2 * 1024 * 1024;

export function SurveyCoverInput({ id, imageKey, file, imageHasError, onImageError, onUrlChange, onFileChange, onError }) {
  const previewRef = useRef(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (!file) return undefined;
    const url = URL.createObjectURL(file);
    if (previewRef.current) previewRef.current.src = url;
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const imageUrl = file ? undefined : resolveSurveyImageUrl(imageKey);
  const uploadedCover = imageKey?.startsWith('/api/v1/public/surveys/');
  const hasPreview = (file || imageUrl) && !imageHasError;

  function selectFile(event) {
    const selected = event.target.files?.[0];
    if (!selected) return;
    if (!['image/png', 'image/jpeg'].includes(selected.type) || selected.size > MAX_FILE_SIZE) {
      onError('Selecciona una imagen PNG o JPEG de hasta 2 MB.');
      event.target.value = '';
      return;
    }
    onError('');
    onFileChange(selected);
    event.target.value = '';
  }

  return (
    <div className="create-survey-image-layout">
      <div className="survey-cover-preview-container">
        <label className={`create-survey-image-preview ${hasPreview ? 'has-image' : ''}`}
          htmlFor={`${id}-file`} aria-label="Seleccionar imagen de portada">
          {hasPreview
            ? <img ref={previewRef} src={imageUrl} alt="Vista previa de la encuesta" onError={onImageError} />
            : <div className="survey-cover-preview-empty"><MdImage aria-hidden="true" /><span>{imageHasError ? 'No se pudo cargar la imagen' : 'Vista previa de la portada'}</span><small>Pulsa para elegir una imagen</small></div>}
          <span className="survey-cover-preview-caption" aria-hidden="true">Portada de la encuesta</span>
        </label>
        {(imageKey || file) && (
          <button type="button" className="create-survey-remove-image" aria-label="Quitar portada" title="Quitar portada"
            onClick={() => { onFileChange(null); onUrlChange(''); fileInputRef.current?.focus(); }}>
            <MdClose aria-hidden="true" />
          </button>
        )}
      </div>
      <div className="survey-cover-controls">
        <div className="create-survey-field survey-cover-file-field">
          <span className="survey-cover-field-label">Imagen del dispositivo</span>
          <div className="survey-cover-file-picker">
            <input ref={fileInputRef} id={`${id}-file`} className="survey-cover-file-input" type="file" accept="image/png,image/jpeg"
              aria-label="Imagen del dispositivo" aria-describedby={`${id}-file-help ${id}-file-status`} onChange={selectFile} />
            <label className="survey-cover-file-button" htmlFor={`${id}-file`}><MdUploadFile aria-hidden="true" />{file ? 'Cambiar imagen' : 'Examinar imágenes'}</label>
            <div className="survey-cover-file-meta">
              <span id={`${id}-file-status`} className={`survey-cover-file-status ${file ? 'has-file' : ''}`} aria-live="polite">
                {file ? file.name : uploadedCover ? 'Portada guardada' : 'Ningún archivo seleccionado'}
              </span>
              <small id={`${id}-file-help`} className="survey-cover-file-help">PNG o JPEG · Máximo 2 MB</small>
            </div>
          </div>
        </div>
        <div className="create-survey-field survey-cover-url-field">
          <label htmlFor={`${id}-url`}>URL de la imagen</label>
          <input id={`${id}-url`} type="url" value={uploadedCover ? '' : imageKey || ''} placeholder="https://ejemplo.com/imagen.jpg" onChange={(event) => { onFileChange(null); onUrlChange(event.target.value); }} />
        </div>
      </div>
    </div>
  );
}
