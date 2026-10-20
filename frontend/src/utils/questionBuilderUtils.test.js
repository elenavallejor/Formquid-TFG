import { describe, expect, it } from 'vitest';

import {
  INITIAL_QUESTION_STATE,
  buildCreateFixedQuestionPayload,
  buildQuestionDraftPayload,
  getQuestionBuilderValidationError,
  getScaleValues,
  parseIntegerValue,
  parseNumberValue,
} from './questionBuilderUtils.js';

function draft(changes = {}) {
  return { ...INITIAL_QUESTION_STATE, statement: '¿Cómo estás?', ...changes };
}

describe('questionBuilderUtils', () => {
  it('valida enunciado, longitud y rangos numéricos', () => {
    expect(getQuestionBuilderValidationError(draft({ statement: '  ' }))).toMatch(/enunciado/);
    expect(getQuestionBuilderValidationError(draft({ maxLength: 0 }))).toMatch(/longitud máxima/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'NUMBER', minValue: 5, maxValue: 1 }))).toMatch(/mínimo debe ser menor/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'NUMBER', step: 0 }))).toMatch(/paso debe ser mayor/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'NUMBER', minValue: '' }))).toMatch(/rango debe tener/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'NUMBER', minValue: 0, maxValue: 10, step: 2 }))).toBe('');
  });

  it('aplica los límites visuales y de filas a escalas y matrices', () => {
    expect(getQuestionBuilderValidationError(draft({ questionType: 'SCALE', minValue: 1, maxValue: 40, step: 1 }))).toMatch(/máximo 30 valores/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'SCALE', scaleDisplayType: 'STARS', minValue: 0 }))).toMatch(/deben empezar en 1/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'SCALE', scaleDisplayType: 'STARS', maxValue: 11 }))).toMatch(/no puede superar 10/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'SCALE', scaleDisplayType: 'FACES', maxValue: 6 }))).toMatch(/no puede superar 5/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'MATRIX_SCALE', matrixRows: [' ', { label: '' }] }))).toMatch(/al menos una fila/);
    expect(getScaleValues({ minValue: 1, maxValue: 2, step: 0.5 })).toEqual([1, 1.5, 2]);
    expect(getScaleValues({ minValue: 5, maxValue: 1, step: 1 })).toEqual([5]);
  });

  it('exige opciones y límites coherentes para respuestas cerradas', () => {
    expect(getQuestionBuilderValidationError(draft({ questionType: 'SINGLE_CHOICE', options: [{ label: 'Única' }] }))).toMatch(/al menos dos opciones/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'MULTIPLE_CHOICE', minSelections: 3, maxSelections: 2 }))).toMatch(/límites de selección/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'MULTIPLE_CHOICE', minSelections: 1, maxSelections: 2 }))).toBe('');
    expect(getQuestionBuilderValidationError(draft({ questionType: 'BOOLEAN', trueLabel: '' }))).toMatch(/dos etiquetas/);
    expect(getQuestionBuilderValidationError(draft({ questionType: 'BOOLEAN', evaluationDirection: 'POSITIVE', trueFactor: '', falseFactor: 0 }), { includeEvaluation: true })).toMatch(/necesitan sus factores/);
  });

  it('normaliza texto y números al crear preguntas y opciones', () => {
    const text = buildQuestionDraftPayload(draft({ statement: '  Pregunta  ', annotation: '  Nota  ', maxLength: '120' }));
    expect(text).toMatchObject({ statement: 'Pregunta', annotation: 'Nota', evaluationDirection: 'NEUTRAL', evaluationWeight: null, settings: { maxLength: 120 }, options: [], matrixRows: [] });
    expect(buildCreateFixedQuestionPayload(draft()).question.statement).toBe('¿Cómo estás?');
    const choices = buildQuestionDraftPayload(draft({
      questionType: 'MULTIPLE_CHOICE', evaluationDirection: 'POSITIVE', evaluationWeight: '2,5',
      hasOtherOption: true, options: [{ label: '  Primera  ', evaluationFactor: '1,5' }, { label: 'Segunda', evaluationFactor: 0 }],
    }), { includeEvaluation: true });
    expect(choices.evaluationWeight).toBe(2.5);
    expect(choices.options).toEqual([
      { label: '  Primera  ', position: 1, other: false, evaluationFactor: 1.5 },
      { label: 'Segunda', position: 2, other: false, evaluationFactor: 0 },
      { label: 'Otro', position: 3, other: true, evaluationFactor: 0 },
    ]);
    expect(choices.settings).toEqual({ minSelections: 1, maxSelections: 2 });
  });

  it('serializa ajustes propios de números, matrices y respuestas booleanas', () => {
    const number = buildQuestionDraftPayload(draft({ questionType: 'NUMBER', minValue: '1,5', maxValue: '10', step: '0,5' }));
    expect(number.settings).toEqual({ minValue: 1.5, maxValue: 10, step: 0.5 });
    const matrix = buildQuestionDraftPayload(draft({ questionType: 'MATRIX_SCALE', matrixRows: [{ label: 'Rapidez' }, { label: '  ' }], scaleDisplayType: 'FACES' }));
    expect(matrix.matrixRows).toEqual([{ label: 'Rapidez', position: 1 }]);
    expect(matrix.settings.scaleDisplayType).toBe('FACES');
    const boolean = buildQuestionDraftPayload(draft({ questionType: 'BOOLEAN', evaluationDirection: 'POSITIVE', trueFactor: '1', falseFactor: '0' }), { includeEvaluation: true });
    expect(boolean.settings).toMatchObject({ trueLabel: 'Sí', falseLabel: 'No', trueEvaluationFactor: 1, falseEvaluationFactor: 0 });
    expect(parseNumberValue('1,5')).toBe(1.5);
    expect(parseIntegerValue('5.8')).toBe(5);
  });
});
