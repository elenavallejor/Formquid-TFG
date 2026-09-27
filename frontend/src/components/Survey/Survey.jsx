import { useMemo, useState } from 'react';
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

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === '';
}

function isOtherAnswer(value) {
  return Boolean(value && typeof value === 'object' && value.isOther);
}

function getQuestionData(item) {
  return item.question
    ? { ...buildBlockQuestionFromCatalog(item.question), isRequired: Boolean(item.required) }
    : { ...item, isRequired: Boolean(item.isRequired ?? item.required) };
}

function toDomId(value) {
  return String(value).replace(/[^a-zA-Z0-9_-]/g, '-');
}

function getBlockContentItems(block) {
  return Array.isArray(block.contentItems) ? block.contentItems : getBlockItems(block);
}

function renderTextItem(item, path) {
  return <div key={item.localId || item.id || path} className="survey-content-text">{item.textBody}</div>;
}

function collectQuestions(items) {
  const questions = [];

  function addQuestion(item, path) {
    const data = getQuestionData(item);
    const answerKey = item.localId || item.id || path;
    const questionNumber = questions.length + 1;
    questions.push({
      answerKey,
      data,
      idBase: `survey-question-${questionNumber}-${toDomId(answerKey)}`,
      path,
      questionNumber,
    });
  }

  items.forEach((item, index) => {
    const path = `item-${index}`;
    if (item.itemType === 'QUESTION') {
      addQuestion(item, path);
      return;
    }
    if (item.itemType !== 'BLOCK') return;

    const block = item.block || item;
    getBlockContentItems(block).forEach((blockItem, blockIndex) => {
      if (blockItem.itemType === 'QUESTION') {
        addQuestion(blockItem, `${path}-block-${blockIndex}`);
      }
    });
  });

  return questions;
}

function getSetting(data, name) {
  return data?.[name] ?? data?.settings?.[name];
}

function getNumberSetting(data, name) {
  const value = getSetting(data, name);
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function getQuestionValidationError(data, value) {
  const required = Boolean(data.isRequired);
  const type = data.questionType;

  if (type === 'TEXT') {
    if (required && isBlank(value)) return 'Escribe una respuesta.';
    const maxLength = getNumberSetting(data, 'maxLength');
    if (!isBlank(value) && maxLength !== null && String(value).length > maxLength) {
      return `La respuesta no puede superar ${maxLength} caracteres.`;
    }
    return '';
  }

  if (type === 'NUMBER') {
    if (isBlank(value)) return required ? 'Introduce un número.' : '';
    const numericValue = Number(value);
    if (!Number.isFinite(numericValue)) return 'Introduce un número válido.';

    const minValue = getNumberSetting(data, 'minValue');
    const maxValue = getNumberSetting(data, 'maxValue');
    const step = getNumberSetting(data, 'step');
    if (minValue !== null && numericValue < minValue) return `El valor mínimo es ${minValue}.`;
    if (maxValue !== null && numericValue > maxValue) return `El valor máximo es ${maxValue}.`;
    if (step !== null && step > 0) {
      const base = minValue ?? 0;
      const steps = (numericValue - base) / step;
      if (Math.abs(steps - Math.round(steps)) > 1e-9) return `Usa incrementos de ${step}.`;
    }
    return '';
  }

  if (type === 'SINGLE_CHOICE') {
    if (required && isBlank(value)) return 'Selecciona una opción.';
    if (isOtherAnswer(value) && isBlank(value.otherText)) return 'Especifica la respuesta «Otro».';
    return '';
  }

  if (type === 'MULTIPLE_CHOICE') {
    const selections = Array.isArray(value) ? value : [];
    const minSelections = getNumberSetting(data, 'minSelections');
    const maxSelections = getNumberSetting(data, 'maxSelections');
    if (selections.length === 0 && !required) return '';
    const effectiveMinimum = minSelections !== null && minSelections > 0
      ? minSelections
      : required ? 1 : 0;

    if (selections.length < effectiveMinimum) {
      return effectiveMinimum === 1
        ? 'Selecciona al menos una opción.'
        : `Selecciona al menos ${effectiveMinimum} opciones.`;
    }
    if (maxSelections !== null && maxSelections > 0 && selections.length > maxSelections) {
      return `Selecciona como máximo ${maxSelections} opciones.`;
    }
    if (selections.some((selection) => isOtherAnswer(selection) && isBlank(selection.otherText))) {
      return 'Especifica la respuesta «Otro».';
    }
    return '';
  }

  if (type === 'SCALE') {
    return required && isBlank(value) ? 'Selecciona un valor de la escala.' : '';
  }

  if (type === 'MATRIX_SCALE') {
    const rows = Array.isArray(data.matrixRows) ? data.matrixRows : [];
    const answeredRows = value && typeof value === 'object' ? Object.keys(value).length : 0;
    if (answeredRows === 0) return required ? 'Responde todas las filas de la matriz.' : '';
    if (answeredRows < rows.length) return 'Responde todas las filas de la matriz.';
    return '';
  }

  if (type === 'BOOLEAN') {
    return required && value !== 'true' && value !== 'false' ? 'Selecciona una de las dos opciones.' : '';
  }

  return required && isBlank(value) ? 'Responde esta pregunta.' : '';
}

function getFocusTargetId(question) {
  return question.data.questionType === 'TEXT' || question.data.questionType === 'NUMBER'
    ? `${question.idBase}-control`
    : `${question.idBase}-group`;
}

export function Survey({ data, mode = SURVEY_MODES.VIEW, isPreview = false, onClosePreview, onSubmit }) {
  const [answers, setAnswers] = useState({});
  const [errors, setErrors] = useState({});
  const items = useMemo(() => data?.items || data?.contentItems || [], [data?.items, data?.contentItems]);
  const interactive = mode === SURVEY_MODES.RESPOND;
  const fontFamily = FONT_FAMILIES[data?.fontFamily] || FONT_FAMILIES.TIMES_NEW_ROMAN;
  const questions = useMemo(() => collectQuestions(items), [items]);
  const questionByPath = useMemo(
    () => new Map(questions.map((question) => [question.path, question])),
    [questions],
  );

  function renderQuestion(path, headingLevel = 2) {
    const question = questionByPath.get(path);
    if (!question) return null;

    const { answerKey, data: questionData, idBase, questionNumber } = question;
    return (
      <Question
        key={`${path}-${answerKey}`}
        questionNumber={questionNumber}
        headingLevel={headingLevel}
        data={questionData}
        interactive={interactive}
        value={answers[answerKey] ?? null}
        idBase={idBase}
        errorMessage={errors[answerKey] || ''}
        onChange={(value) => {
          setAnswers((current) => ({ ...current, [answerKey]: value }));
          setErrors((current) => {
            if (!current[answerKey]) return current;
            const nextError = getQuestionValidationError(questionData, value);
            const nextErrors = { ...current };
            if (nextError) nextErrors[answerKey] = nextError;
            else delete nextErrors[answerKey];
            return nextErrors;
          });
        }}
      />
    );
  }

  function renderItem(item, index) {
    const path = `item-${index}`;
    if (item.itemType === 'TEXT') {
      return renderTextItem(item, path);
    }
    if (item.itemType === 'QUESTION') return renderQuestion(path);
    if (item.itemType !== 'BLOCK') return null;

    const block = item.block || item;
    return (
      <section key={item.localId || item.id || path} className="survey-response-block">
        <header>
          <h2>{block.name || `Bloque ${index + 1}`}</h2>
          {block.description && <p>{block.description}</p>}
        </header>
        <div className="survey-response-block-items">
          {getBlockContentItems(block).map((blockItem, blockIndex) => {
            const blockPath = `${path}-block-${blockIndex}`;
            if (blockItem.itemType === 'TEXT') {
              return renderTextItem(blockItem, blockPath);
            }
            return renderQuestion(blockPath, 3);
          })}
        </div>
      </section>
    );
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (isPreview) return;

    const nextErrors = Object.fromEntries(
      questions
        .map((question) => [question.answerKey, getQuestionValidationError(question.data, answers[question.answerKey])])
        .filter(([, error]) => Boolean(error)),
    );
    setErrors(nextErrors);

    const firstInvalidQuestion = questions.find((question) => nextErrors[question.answerKey]);
    if (firstInvalidQuestion) {
      window.requestAnimationFrame(() => {
        document.getElementById(getFocusTargetId(firstInvalidQuestion))?.focus();
      });
      return;
    }

    onSubmit?.(answers);
  }

  const invalidQuestions = questions.filter((question) => errors[question.answerKey]);

  return (
    <form className={`survey-component ${data?.fontFamily === 'OPEN_DYSLEXIC' ? 'font-open-dyslexic' : ''}`} style={{ fontFamily }} noValidate onSubmit={handleSubmit}>
      {isPreview && <div className="survey-preview-notice" role="status">Estás previsualizando la encuesta. Las respuestas introducidas no se guardarán.</div>}
      {data?.imageKey && <img className="survey-response-cover" src={data.imageKey} alt="" />}
      <header className="survey-response-header">
        <h1>{data?.title || 'Encuesta sin título'}</h1>
        {data?.description && <p>{data.description}</p>}
        {Array.isArray(data?.tags) && data.tags.length > 0 && <ul>{data.tags.map((tag) => <li key={tag}>#{tag}</li>)}</ul>}
      </header>

      {invalidQuestions.length > 0 && (
        <section className="survey-response-validation" role="alert" aria-labelledby="survey-validation-title">
          <h2 id="survey-validation-title">
            {invalidQuestions.length === 1
              ? 'Hay un error que debes corregir'
              : `Hay ${invalidQuestions.length} errores que debes corregir`}
          </h2>
          <ul>
            {invalidQuestions.map((question) => {
              const focusTargetId = getFocusTargetId(question);
              return (
                <li key={`${question.path}-error`}>
                  <a
                    href={`#${focusTargetId}`}
                    onClick={(event) => {
                      event.preventDefault();
                      document.getElementById(focusTargetId)?.focus();
                    }}
                  >
                    Pregunta {question.questionNumber}: {errors[question.answerKey]}
                  </a>
                </li>
              );
            })}
          </ul>
        </section>
      )}

      <div className="survey-response-content">
        {items.length > 0 ? items.map(renderItem) : <p className="survey-response-empty">Esta encuesta todavía no contiene elementos.</p>}
      </div>

      {interactive && <footer className="survey-response-actions">
        {isPreview
          ? <button type="button" onClick={onClosePreview}>Cerrar previsualización</button>
          : <button type="submit">Enviar respuestas</button>}
      </footer>}
    </form>
  );
}
