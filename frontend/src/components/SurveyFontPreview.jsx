import './SurveyFontPreview.css';

export function SurveyFontPreview({ fontFamily, isOpenDyslexic, inputId }) {
  return (
    <div className={`survey-font-preview ${isOpenDyslexic ? 'is-open-dyslexic' : ''}`} aria-live="polite">
      <span>Vista previa</span>
      <div className="survey-font-preview-sample" style={{ fontFamily }}>
        <label htmlFor={inputId}>¿Cómo utilizas la tecnología en tu día a <span className="survey-font-preview-ending">día? <strong>*</strong></span></label>
        <input id={inputId} type="text" placeholder="Tu respuesta" disabled />
      </div>
    </div>
  );
}
