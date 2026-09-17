import { useEffect } from 'react';
import { MdClose } from 'react-icons/md';

import { Survey } from '../Survey/Survey.jsx';
import { SURVEY_MODES } from '../Survey/SurveyModes.js';

import './SurveyPreviewModal.css';

export function SurveyPreviewModal({ isOpen, survey, structure, onClose }) {
  useEffect(() => {
    if (!isOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose();
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="survey-preview-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="survey-preview-modal" role="dialog" aria-modal="true" aria-labelledby="survey-preview-title">
        <header className="survey-preview-modal-header">
          <div>
            <span>Modo de previsualización</span>
            <h2 id="survey-preview-title">VISTA DEL ENCUESTADO</h2>
          </div>
          <button type="button" aria-label="Cerrar previsualización" onClick={onClose}><MdClose aria-hidden="true" /></button>
        </header>
        <div className="survey-preview-modal-body">
          <Survey data={{ ...survey, items: structure?.items || [] }} mode={SURVEY_MODES.RESPOND} isPreview onClosePreview={onClose} />
        </div>
      </section>
    </div>
  );
}
