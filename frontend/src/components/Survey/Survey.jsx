import { useMemo, useState } from 'react';
import '@fontsource/lexend/400.css';
import '@fontsource/opendyslexic/400.css';

import { buildBlockQuestionFromCatalog, getBlockItems } from '../../utils/blockBuilderUtils.js';
import { getOptionAnswerValue } from '../../utils/questionBuilderUtils.js';
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
  if (!item.question) {
    return { ...item, isRequired: Boolean(item.isRequired ?? item.required) };
  }

  return {
    ...buildBlockQuestionFromCatalog(item.question),
    id: item.question.id,
    options: Array.isArray(item.question.options)
      ? item.question.options.map((option) => ({ ...option }))
      : [],
    matrixRows: Array.isArray(item.question.matrixRows)
      ? item.question.matrixRows.map((row) => ({ ...row }))
      : [],
    isRequired: Boolean(item.required),
  };
}

function toDomId(value) {
  return String(value).replace(/[^a-zA-Z0-9_-]/g, '-');
}

function getBlockContentItems(block) {
  return Array.isArray(block.contentItems) ? block.contentItems : getBlockItems(block);
}

function getAlphabeticLabel(index) {
  let value = index + 1;
  let label = '';

  while (value > 0) {
    value -= 1;
    label = String.fromCharCode(65 + (value % 26)) + label;
    value = Math.floor(value / 26);
  }

  return label;
}

function collectBlockLabels(items) {
  const labels = new Map();

  items.forEach((item, index) => {
    if (item.itemType === 'BLOCK') {
      labels.set(`item-${index}`, getAlphabeticLabel(labels.size));
    }
  });

  return labels;
}

function renderTextItem(item, path) {
  return <p key={item.localId || item.id || path} className="survey-content-text">{item.textBody}</p>;
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

function buildSelectedOptions(data, value) {
  const selections = Array.isArray(value) ? value : [value];

  return selections.flatMap((selection) => {
    if (isOtherAnswer(selection)) {
      const otherOption = (data.options || []).find(
        (option) => Boolean(option?.other ?? option?.isOther),
      );
      const optionId = selection.optionId ?? otherOption?.id;
      return optionId != null
        ? [{ optionId, otherText: String(selection.otherText || '').trim() }]
        : [];
    }

    const option = (data.options || []).find(
      (candidate, index) => getOptionAnswerValue(candidate, index) === selection,
    );
    return option?.id != null ? [{ optionId: option.id }] : [];
  });
}

function buildParticipationAnswer(question, value) {
  const { data } = question;
  const baseAnswer = { questionId: data.id };

  if (!data.id) return null;

  if (data.questionType === 'TEXT') {
    return isBlank(value) ? null : { ...baseAnswer, textValue: String(value) };
  }

  if (data.questionType === 'NUMBER' || data.questionType === 'SCALE') {
    return isBlank(value) ? null : { ...baseAnswer, numericValue: Number(value) };
  }

  if (data.questionType === 'BOOLEAN') {
    return value !== 'true' && value !== 'false'
      ? null
      : { ...baseAnswer, booleanValue: value === 'true' };
  }

  if (data.questionType === 'SINGLE_CHOICE') {
    if (isBlank(value)) return null;
    const selectedOptions = buildSelectedOptions(data, value);
    return selectedOptions.length > 0 ? { ...baseAnswer, selectedOptions } : null;
  }

  if (data.questionType === 'MULTIPLE_CHOICE') {
    if (!Array.isArray(value) || value.length === 0) return null;
    const selectedOptions = buildSelectedOptions(data, value);
    return selectedOptions.length > 0 ? { ...baseAnswer, selectedOptions } : null;
  }

  if (data.questionType === 'MATRIX_SCALE') {
    const matrixValues = Object.entries(value || {}).flatMap(([rowIndex, numericValue]) => {
      const matrixRowId = data.matrixRows?.[Number(rowIndex)]?.id;
      return matrixRowId ? [{ matrixRowId, numericValue: Number(numericValue) }] : [];
    });
    return matrixValues.length > 0 ? { ...baseAnswer, matrixValues } : null;
  }

  return null;
}

function buildParticipationAnswers(questions, answers) {
  return questions
    .map((question) => buildParticipationAnswer(question, answers[question.answerKey]))
    .filter(Boolean);
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

function requiredMessage(instruction) {
  return `Esta pregunta es obligatoria: ${instruction}`;
}

function getQuestionValidationError(data, value) {
  const required = Boolean(data.isRequired);
  const type = data.questionType;

  if (type === 'TEXT') {
    if (required && isBlank(value)) return requiredMessage('escribe una respuesta.');
    const maxLength = getNumberSetting(data, 'maxLength');
    if (!isBlank(value) && maxLength !== null && String(value).length > maxLength) {
      return `La respuesta no puede superar ${maxLength} caracteres.`;
    }
    return '';
  }

  if (type === 'NUMBER') {
    if (isBlank(value)) return required ? requiredMessage('introduce un número.') : '';
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
    if (required && isBlank(value)) return requiredMessage('selecciona una opción.');
    if (isOtherAnswer(value) && isBlank(value.otherText)) return 'Especifica la respuesta «Otro».';
    return '';
  }

  if (type === 'MULTIPLE_CHOICE') {
    const selections = Array.isArray(value) ? value : [];
    const minSelections = getNumberSetting(data, 'minSelections');
    const maxSelections = getNumberSetting(data, 'maxSelections');
    const effectiveMinimum = minSelections !== null && minSelections > 0
      ? minSelections
      : required ? 1 : 0;
    if (selections.length === 0) {
      if (!required) return '';
      return requiredMessage(effectiveMinimum === 1
        ? 'selecciona al menos una opción.'
        : `selecciona al menos ${effectiveMinimum} opciones.`);
    }

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
    return required && isBlank(value)
      ? requiredMessage('selecciona un valor de la escala.')
      : '';
  }

  if (type === 'MATRIX_SCALE') {
    const rows = Array.isArray(data.matrixRows) ? data.matrixRows : [];
    const answeredRows = value && typeof value === 'object' ? Object.keys(value).length : 0;
    if (answeredRows === 0) {
      return required ? requiredMessage('responde todas las filas de la matriz.') : '';
    }
    if (answeredRows < rows.length) return 'Responde todas las filas de la matriz.';
    return '';
  }

  if (type === 'BOOLEAN') {
    return required && value !== 'true' && value !== 'false'
      ? requiredMessage('selecciona una de las dos opciones.')
      : '';
  }

  return required && isBlank(value) ? requiredMessage('responde esta pregunta.') : '';
}

function getFocusTargetId(question) {
  return question.data.questionType === 'TEXT' || question.data.questionType === 'NUMBER'
    ? `${question.idBase}-control`
    : `${question.idBase}-group`;
}

export function Survey({
  data,
  mode = SURVEY_MODES.VIEW,
  isPreview = false,
  isSubmitting = false,
  onClosePreview,
  onSubmit,
}) {
  const [answers, setAnswers] = useState({});
  const [errors, setErrors] = useState({});
  const items = useMemo(() => data?.items || data?.contentItems || [], [data?.items, data?.contentItems]);
  const interactive = mode === SURVEY_MODES.RESPOND;
  const fontFamily = FONT_FAMILIES[data?.fontFamily] || FONT_FAMILIES.TIMES_NEW_ROMAN;
  const questions = useMemo(() => collectQuestions(items), [items]);
  const hasRequiredQuestions = questions.some((question) => question.data.isRequired);
  const blockLabels = useMemo(() => collectBlockLabels(items), [items]);
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
        onClearError={() => {
          setErrors((current) => {
            const nextErrors = { ...current };
            delete nextErrors[answerKey];
            return nextErrors;
          });
        }}
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
    const blockLabel = blockLabels.get(path);
    const blockHeadingId = `survey-block-${blockLabel.toLowerCase()}-${toDomId(item.localId || item.id || path)}`;
    return (
      <section
        key={item.localId || item.id || path}
        className="survey-response-block"
        aria-labelledby={blockHeadingId}
      >
        <header>
          <h2 id={blockHeadingId}>
            <span className="survey-block-prefix" aria-hidden="true">{blockLabel}.</span>
            <span className="sr-only">Bloque {blockLabel}: </span>
            <span>{block.name || 'Sección sin título'}</span>
          </h2>
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
        const target = document.getElementById(getFocusTargetId(firstInvalidQuestion));
        if (!target) return;
        const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        target.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'center' });
        target.focus({ preventScroll: true });
      });
      return;
    }

    onSubmit?.(buildParticipationAnswers(questions, answers));
  }

  return (
    <form
      className={`survey-component ${data?.fontFamily === 'OPEN_DYSLEXIC' ? 'font-open-dyslexic' : ''}`}
      style={{ fontFamily }}
      aria-describedby={hasRequiredQuestions ? 'survey-required-note' : undefined}
      noValidate
      onSubmit={handleSubmit}
    >
      {isPreview && <div className="survey-preview-notice" role="status">Estás previsualizando la encuesta. Las respuestas introducidas no se guardarán.</div>}
      <section className="survey-response-introduction" aria-labelledby="survey-response-title">
        {data?.imageKey && <img className="survey-response-cover" src={data.imageKey} alt="" />}
        <header className="survey-response-header">
          <h1 id="survey-response-title">{data?.title || 'Encuesta sin título'}</h1>
          {data?.description && <p>{data.description}</p>}
          {Array.isArray(data?.tags) && data.tags.length > 0 && <ul>{data.tags.map((tag) => <li key={tag}>#{tag}</li>)}</ul>}
        </header>

        {hasRequiredQuestions && (
          <p id="survey-required-note" className="survey-required-note">
            <span aria-hidden="true">*</span>
            <span>Las preguntas marcadas con un asterisco son obligatorias.</span>
          </p>
        )}
      </section>

      <section className="survey-response-questions" aria-label="Preguntas de la encuesta">
        <div className="survey-response-content">
          {items.length > 0 ? items.map(renderItem) : <p className="survey-response-empty">Esta encuesta todavía no contiene elementos.</p>}
        </div>

        {interactive && <footer className="survey-response-actions">
          {isPreview
            ? <button type="button" onClick={onClosePreview}>Cerrar previsualización</button>
            : <button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Enviando...' : 'Enviar respuestas'}</button>}
        </footer>}
      </section>
    </form>
  );
}
