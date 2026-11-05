import { describe, it, expect } from 'vitest';
import { INITIAL_QUESTION_STATE as initial, getQuestionBuilderValidationError as validate, buildQuestionDraftPayload, getScaleValues, parseNumberValue, parseIntegerValue } from '../questionBuilderUtils.js';
import { INITIAL_REGISTER_FORM_DATA, validateRegisterForm, REGISTER_FORM_LIMITS, hasRegisterFormErrors } from '../registerFormValidation.js';
import { getDragData, getDropIndex, reorderItems } from '../builderDragUtils.js';
const draft = changes => ({ ...initial, statement: 'Pregunta', ...changes });
describe('validación de preguntas: datos incompletos', () => {
  it.each([
    [{ questionType: 'UNKNOWN' }, /tipo de pregunta/],
    [{ evaluationDirection: 'UNKNOWN' }, /dirección/],
    [{ evaluationDirection: 'POSITIVE' }, /texto deben ser neutrales/],
    [{ questionType: 'BOOLEAN', falseLabel: '' }, /dos etiquetas/],
    [{ questionType: 'BOOLEAN', evaluationDirection: 'POSITIVE', evaluationWeight: 1, falseFactor: '' }, /factores/],
    [{ questionType: 'SINGLE_CHOICE', evaluationDirection: 'POSITIVE', evaluationWeight: 1, options: [{ label: 'A', factor: '' }, { label: 'B', factor: 0 }] }, /factor/],
    [{ questionType: 'NUMBER', maxValue: '' }, /rango/],
    [{ questionType: 'NUMBER', step: '' }, /rango/],
    [{ questionType: 'SCALE', scaleDisplayType: 'STARS', step: 0.5 }, /avanzar/],
    [{ questionType: 'SCALE', scaleDisplayType: 'FACES', maxValue: 3.5 }, /avanzar/],
    [{ questionType: 'MULTIPLE_CHOICE', minSelections: -1 }, /límites/],
    [{ questionType: 'MULTIPLE_CHOICE', maxSelections: 0 }, /límites/],
    [{ questionType: 'MULTIPLE_CHOICE', maxSelections: 3 }, /límites/],
    [{ questionType: 'SINGLE_CHOICE', options: null }, /dos opciones/],
    [{ questionType: 'MATRIX_SCALE', matrixRows: null }, /al menos una fila/],
  ])('rechaza %j', (changes, error) => expect(validate(draft(changes), { includeEvaluation: true })).toMatch(error));
  it('normaliza ajustes vacíos sin crear factores neutrales ni duplicar Otro', () => {
    expect(buildQuestionDraftPayload(draft({ questionType: 'SINGLE_CHOICE', hasOtherOption: true, options: [{ text: 'Alternativa', other: true }] })).options).toEqual([{ label: 'Alternativa', position: 1, other: true }]);
    expect(buildQuestionDraftPayload(draft({ questionType: 'UNKNOWN' })).settings).toEqual({});
    expect(buildQuestionDraftPayload(draft({ questionType: 'NUMBER', minValue: '', maxValue: undefined, step: 'abc' })).settings).toEqual({});
    expect(buildQuestionDraftPayload(draft({ questionType: 'BOOLEAN' })).settings).not.toHaveProperty('falseEvaluationFactor');
    expect(getScaleValues({ minValue: '', maxValue: '', step: '' })).toEqual([1, 2, 3, 4, 5]);
    expect(getScaleValues({ minValue: 0, maxValue: 1, step: 0 })).toEqual([0]);
    expect(getScaleValues({ minValue: 0, maxValue: 1, step: 0.3 })).toEqual([0, 0.3, 0.6, 0.9]);
    expect(parseNumberValue('')).toBe(''); expect(parseNumberValue('abc')).toBe('');
    expect(parseIntegerValue('')).toBe(''); expect(parseIntegerValue('abc')).toBe('');
  });
});
describe('límites de registro', () => {
  const valid = { ...INITIAL_REGISTER_FORM_DATA, name: 'Ana', email: 'ana@example.com', institution: 'Universidad', country: 'ES', motivation: 'Investigación', password: 'password123', confirmPassword: 'password123' };
  it.each(['name', 'email', 'institution', 'country', 'motivation', 'password'])('rechaza %s demasiado largo', key => {
    const value = key === 'email' ? `${'a'.repeat(REGISTER_FORM_LIMITS.email)}@example.com` : 'a'.repeat(REGISTER_FORM_LIMITS[key] + 1);
    expect(validateRegisterForm({ ...valid, [key]: value })[key]).toMatch(/superar/);
  });
  it('distingue contraseña corta, confirmación vacía y distinta', () => {
    expect(validateRegisterForm({ ...valid, password: 'short' }).password).toMatch(/al menos/);
    expect(validateRegisterForm({ ...valid, confirmPassword: '' }).confirmPassword).toMatch(/Confirma/);
    expect(validateRegisterForm({ ...valid, confirmPassword: 'different' }).confirmPassword).toMatch(/no coinciden/);
    expect(hasRegisterFormErrors(validateRegisterForm(valid))).toBe(false);
  });
});
describe('arrastre y ordenación: entradas inválidas', () => {
  it.each(['', '{invalid'])('ignora datos de arrastre %s', value => expect(getDragData({ dataTransfer: { getData: () => value } })).toBeNull());
  it.each([[null, 0], [0, null], [-1, 0], [3, 0], [0, 0], [0, 1]])('conserva el orden ante movimiento %j → %j', (from, to) => {
    const items = ['A', 'B']; expect(reorderItems(items, from, to)).toBe(items);
  });
  it('calcula la inserción antes y después del centro', () => {
    const currentTarget = { getBoundingClientRect: () => ({ top: 10, height: 20 }) };
    expect(getDropIndex({ currentTarget, clientY: 15 }, 2)).toBe(2);
    expect(getDropIndex({ currentTarget, clientY: 25 }, 2)).toBe(3);
    expect(reorderItems(['A', 'B'], 1, 0)).toEqual(['B', 'A']);
  });
});
