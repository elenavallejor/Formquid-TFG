import { useState } from 'react';
import '@fontsource/lexend/400.css';
import '@fontsource/opendyslexic/400.css';

import { buildBlockQuestionFromCatalog, getBlockItems } from '../../utils/blockBuilderUtils.js';
import { Question } from '../Question/Question.jsx';
import { SURVEY_MODES } from './SurveyModes.js';

import './Survey.css';

const FONT_FAMILIES = {
  TIMES_NEW_ROMAN: '"Computer Modern", "Latin Modern Roman", Georgia, "Times New Roman", serif',
  OPEN_DYSLEXIC: 'OpenDyslexic, sans-serif',
  LEXEND: 'Lexend, sans-serif',
  ARIAL: 'Arial, sans-serif',
};

function hasAnswer(value) {
  if (Array.isArray(value)) return value.length > 0;
  if (value && typeof value === 'object') return Object.keys(value).length > 0;
  return value !== null && value !== undefined && String(value).trim() !== '';
}

export function Survey({ data, mode = SURVEY_MODES.VIEW, isPreview = false, onClosePreview, onSubmit }) {
  const [answers, setAnswers] = useState({});
  const [validationMessage, setValidationMessage] = useState('');
  const items = data?.items || data?.contentItems || [];
  const interactive = mode === SURVEY_MODES.RESPOND;
  const fontFamily = FONT_FAMILIES[data?.fontFamily] || FONT_FAMILIES.TIMES_NEW_ROMAN;
  let questionNumber = 0;
  const requiredQuestions = [];

  function renderQuestion(item, path) {
    questionNumber += 1;
    const answerKey = item.localId || item.id || path;
    const questionData = item.question
      ? { ...buildBlockQuestionFromCatalog(item.question), isRequired: Boolean(item.required) }
      : { ...item, isRequired: Boolean(item.isRequired ?? item.required) };
    if (questionData.isRequired) requiredQuestions.push(answerKey);
    return (
      <Question
        key={answerKey}
        questionNumber={questionNumber}
        data={questionData}
        interactive={interactive}
        value={answers[answerKey] ?? null}
        onChange={(value) => {
          setAnswers((current) => ({ ...current, [answerKey]: value }));
          setValidationMessage('');
        }}
      />
    );
  }

  function renderItem(item, index) {
    const path = `item-${index}`;
    if (item.itemType === 'TEXT') {
      return <div key={item.localId || item.id || path} className="survey-content-text">{item.textBody}</div>;
    }
    if (item.itemType === 'QUESTION') return renderQuestion(item, path);
    if (item.itemType !== 'BLOCK') return null;

    const block = item.block || item;
    return (
      <section key={item.localId || item.id || path} className="survey-response-block">
        <header>
          <h2>{block.name || `Bloque ${index + 1}`}</h2>
          {block.description && <p>{block.description}</p>}
        </header>
        <div className="survey-response-block-items">
          {(Array.isArray(block.contentItems) ? block.contentItems : getBlockItems(block)).map((blockItem, blockIndex) => {
            const blockPath = `${path}-block-${blockIndex}`;
            if (blockItem.itemType === 'TEXT') {
              return <div key={blockItem.localId || blockItem.id || blockPath} className="survey-content-text">{blockItem.textBody}</div>;
            }
            return renderQuestion(blockItem, blockPath);
          })}
        </div>
      </section>
    );
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (isPreview) return;
    if (requiredQuestions.some((key) => !hasAnswer(answers[key]))) {
      setValidationMessage('Completa todas las preguntas obligatorias antes de enviar la encuesta.');
      return;
    }
    onSubmit?.(answers);
  }

  return (
    <form className={`survey-component ${data?.fontFamily === 'OPEN_DYSLEXIC' ? 'font-open-dyslexic' : ''}`} style={{ fontFamily }} onSubmit={handleSubmit}>
      {isPreview && <div className="survey-preview-notice" role="status">Estás previsualizando la encuesta. Las respuestas introducidas no se guardarán.</div>}
      {data?.imageKey && <img className="survey-response-cover" src={data.imageKey} alt="" />}
      <header className="survey-response-header">
        <h1>{data?.title || 'Encuesta sin título'}</h1>
        {data?.description && <p>{data.description}</p>}
        {Array.isArray(data?.tags) && data.tags.length > 0 && <ul>{data.tags.map((tag) => <li key={tag}>#{tag}</li>)}</ul>}
      </header>

      <div className="survey-response-content">
        {items.length > 0 ? items.map(renderItem) : <p className="survey-response-empty">Esta encuesta todavía no contiene elementos.</p>}
      </div>

      {validationMessage && <p className="survey-response-validation" role="alert">{validationMessage}</p>}
      {interactive && <footer className="survey-response-actions">
        {isPreview
          ? <button type="button" onClick={onClosePreview}>Cerrar previsualización</button>
          : <button type="submit">Enviar respuestas</button>}
      </footer>}
    </form>
  );
}
