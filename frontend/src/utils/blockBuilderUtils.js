import { INITIAL_QUESTION_STATE, buildQuestionDraftPayload, getQuestionBuilderValidationError } from './questionBuilderUtils.js';

/**
 * Define el estado inicial, la validación y las transformaciones utilizadas por el constructor de bloques.
 */
export const INITIAL_BLOCK_STATE = {
  name: '',
  description: '',
  questions: [],
};

export function getBlockBuilderValidationError(data) {
  if (!hasText(data?.name)) return 'Debes asignar un nombre al bloque.';
  const items = getBlockItems(data);
  if (!items.some((item) => item.itemType === 'QUESTION'))
    return 'El bloque debe contener al menos una pregunta.';

  let questionNumber = 0;
  for (const item of items) {
    if (item.itemType === 'TEXT') {
      if (!hasText(item.textBody))
        return 'Los elementos de texto no pueden estar vacíos.';
      continue;
    }
    questionNumber += 1;
    const questionError = getQuestionBuilderValidationError(item, false);
    if (questionError) return `Pregunta ${questionNumber}: ${questionError}`;
  }
  return '';
}

export function buildCreateCatalogBlockPayload(data) {
  return {
    name: data.name.trim(),
    description: trimToNull(data.description),
    items: getBlockItems(data).map((item, index) =>
      item.itemType === 'TEXT'
        ? {
            itemType: 'TEXT',
            position: index + 1,
            textBody: item.textBody.trim(),
          }
        : {
            itemType: 'QUESTION',
            position: index + 1,
            required: Boolean(item.isRequired),
            question: buildQuestionDraftPayload(item),
          },
    ),
  };
}

export function buildBlockQuestionFromCatalog(question) {
  const settings = question.settings || {};
  const options = question.options || [];

  return {
    ...INITIAL_QUESTION_STATE,
    statement: question.statement || '',
    annotation: question.annotation || '',
    questionType: question.questionType,
    evaluationDirection: question.evaluationDirection,
    evaluationWeight: question.evaluationDirection === 'NEUTRAL' ? null : question.evaluationWeight,
    maxLength: settings.maxLength ?? INITIAL_QUESTION_STATE.maxLength,
    minValue: settings.minValue ?? INITIAL_QUESTION_STATE.minValue,
    maxValue: settings.maxValue ?? INITIAL_QUESTION_STATE.maxValue,
    step: settings.step ?? INITIAL_QUESTION_STATE.step,
    minSelections: settings.minSelections ?? INITIAL_QUESTION_STATE.minSelections,
    maxSelections: settings.maxSelections ?? INITIAL_QUESTION_STATE.maxSelections,
    leftLabel: settings.leftLabel ?? INITIAL_QUESTION_STATE.leftLabel,
    rightLabel: settings.rightLabel ?? INITIAL_QUESTION_STATE.rightLabel,
    scaleDisplayType: settings.scaleDisplayType ?? INITIAL_QUESTION_STATE.scaleDisplayType,
    trueLabel: settings.trueLabel ?? INITIAL_QUESTION_STATE.trueLabel,
    falseLabel: settings.falseLabel ?? INITIAL_QUESTION_STATE.falseLabel,
    trueFactor: settings.trueEvaluationFactor ?? INITIAL_QUESTION_STATE.trueFactor,
    falseFactor: settings.falseEvaluationFactor ?? INITIAL_QUESTION_STATE.falseFactor,
    fontFamily: settings.fontFamily ?? INITIAL_QUESTION_STATE.fontFamily,
    fontSize: settings.fontSize ?? INITIAL_QUESTION_STATE.fontSize,
    textColor: settings.textColor ?? INITIAL_QUESTION_STATE.textColor,
    buttonShape: settings.buttonShape ?? INITIAL_QUESTION_STATE.buttonShape,
    options: options
      .filter((option) => !option.other)
      .map((option) => ({
        label: option.label,
        evaluationFactor: option.evaluationFactor,
      })),
    hasOtherOption: options.some((option) => option.other),
    matrixRows: (question.matrixRows || []).map((row) => ({
      label: row.label,
    })),
    isRequired: false,
  };
}

export function buildBlockTextItem(textBody = '') {
  return {
    itemType: 'TEXT',
    localId: `text-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    textBody,
  };
}

export function getBlockItems(data) {
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.questions)) {
    return data.questions.map((question) => ({
      ...question,
      itemType: 'QUESTION',
    }));
  }
  return [];
}

function trimToNull(value) {
  return hasText(value) ? String(value).trim() : null;
}

function hasText(value) {
  return typeof value === 'string' && value.trim().length > 0;
}