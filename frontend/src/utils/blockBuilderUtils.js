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
  if (!data.name?.trim()) {
    return 'Debe asignar un nombre al bloque.';
  }

  if (!data.questions?.length) {
    return 'El bloque debe contener al menos una pregunta.';
  }

  for (let index = 0; index < data.questions.length; index += 1) {
    const questionError = getQuestionBuilderValidationError(data.questions[index]);

    if (questionError) {
      return `Pregunta ${index + 1}: ${questionError}`;
    }
  }

  return '';
}

export function buildCreateCatalogBlockPayload(data) {
  return {
    name: data.name.trim(),
    description: data.description?.trim() || null,
    questions: data.questions.map((question, index) => ({
      position: index + 1,
      required: Boolean(question.isRequired),
      question: buildQuestionDraftPayload(question),
    })),
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