import { useRef } from 'react';
import { MdClose } from 'react-icons/md';

import { useModalFocus } from '../../hooks/useModalFocus.js';
import { Survey } from '../Survey/Survey.jsx';
import { SURVEY_MODES } from '../Survey/SurveyModes.js';

import './SurveyPreviewModal.css';

export function SurveyPreviewModal({ isOpen, survey, structure, onClose }) {
  const dialogRef = useRef(null);
  const titleRef = useRef(null);

  useModalFocus({ isOpen, onClose, dialogRef, initialFocusRef: titleRef });

  if (!isOpen) return null;

  return (
    <div className="survey-preview-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section ref={dialogRef} tabIndex={-1} className="survey-preview-modal" role="dialog" aria-modal="true" aria-labelledby="survey-preview-title">
        <header className="survey-preview-modal-header">
          <div>
            <span>Modo de previsualización</span>
            <h2 ref={titleRef} id="survey-preview-title" tabIndex={-1}>VISTA DEL ENCUESTADO</h2>
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
