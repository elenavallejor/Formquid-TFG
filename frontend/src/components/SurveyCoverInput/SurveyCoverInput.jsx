import { useEffect, useRef, useState } from 'react';
import { MdClose, MdImage, MdKeyboardArrowDown, MdPhotoLibrary, MdUploadFile } from 'react-icons/md';

import { resolveSurveyImageUrl } from '../../api/apiClient.js';
import { galleryImages } from './galleryImages.js';

import './SurveyCoverInput.css';

const MAX_FILE_SIZE = 3 * 1024 * 1024;

export function SurveyCoverInput({ id, imageKey, file, imageHasError, onImageError, onUrlChange, onFileChange, onError, onBusyChange, enableGallery = false }) {
  const previewRef = useRef(null);
  const fileInputRef = useRef(null);
  const galleryRef = useRef(null);
  const galleryTriggerRef = useRef(null);
  const [galleryOpen, setGalleryOpen] = useState(false);
  const [galleryBusy, setGalleryBusy] = useState(false);

  useEffect(() => {
    if (!galleryOpen) return undefined;
    const closeOutside = (event) => {
      if (!galleryRef.current?.contains(event.target)) setGalleryOpen(false);
    };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [galleryOpen]);

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
      onError('Selecciona una imagen PNG o JPEG de hasta 3 MB.');
      event.target.value = '';
      return;
    }
    onError('');
    onFileChange(selected);
    event.target.value = '';
  }

  async function selectGalleryImage(image) {
    setGalleryBusy(true);
    onBusyChange?.(true);
    onError('');
    try {
      const response = await fetch(image.url);
      if (!response.ok) throw new Error('No se pudo cargar la imagen.');
      const blob = await response.blob();
      if (blob.size > MAX_FILE_SIZE) throw new Error('La imagen supera el límite de 3 MB.');
      const type = blob.type === 'image/png' ? 'image/png' : 'image/jpeg';
      onFileChange(new File([blob], image.filename, { type }));
      setGalleryOpen(false);
      galleryTriggerRef.current?.focus();
    } catch (error) {
      onError(error.message || 'No se pudo seleccionar la imagen de la galería.');
    } finally {
      setGalleryBusy(false);
      onBusyChange?.(false);
    }
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
              <small id={`${id}-file-help`} className="survey-cover-file-help">PNG o JPEG · Máximo 3 MB</small>
            </div>
          </div>
        </div>
        <div className="create-survey-field survey-cover-url-field">
          <label htmlFor={`${id}-url`}>URL de la imagen</label>
          <input id={`${id}-url`} type="url" value={uploadedCover ? '' : imageKey || ''} placeholder="https://ejemplo.com/imagen.jpg" onChange={(event) => { onFileChange(null); onUrlChange(event.target.value); }} />
        </div>
        {enableGallery && galleryImages.length > 0 && (
          <div className="survey-cover-gallery" ref={galleryRef}
            onKeyDown={(event) => {
              if (event.key === 'Escape' && galleryOpen) {
                event.stopPropagation();
                setGalleryOpen(false);
                galleryTriggerRef.current?.focus();
              }
            }}>
            <button ref={galleryTriggerRef} type="button" className="survey-cover-gallery-trigger"
              aria-expanded={galleryOpen} aria-controls={`${id}-gallery`}
              onClick={() => setGalleryOpen((open) => !open)}>
              <MdPhotoLibrary className="survey-cover-gallery-icon" aria-hidden="true" />
              <span>ABRIR GALERÍA DE FOTOS</span>
              <MdKeyboardArrowDown className="survey-cover-gallery-chevron" aria-hidden="true" />
            </button>
            {galleryOpen && (
              <div id={`${id}-gallery`} className="survey-cover-gallery-menu" role="group" aria-labelledby={`${id}-gallery-title`}>
                <h3 id={`${id}-gallery-title`}>Galería de fotos</h3>
                <p>Elige una foto para tu portada:</p>
                <div className="survey-cover-gallery-grid">
                  {galleryImages.map((image) => (
                    <button type="button" key={image.filename} disabled={galleryBusy}
                      aria-label={`Usar imagen: ${image.label}`} aria-pressed={file?.name === image.filename}
                      onClick={() => selectGalleryImage(image)}>
                      <img src={image.url} alt="" loading="lazy" />
                    </button>
                  ))}
                </div>
                {galleryBusy && <span role="status">Cargando imagen…</span>}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
