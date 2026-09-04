import { MdOutlineTextFields } from 'react-icons/md';
import { TiSortNumerically, TiInputChecked } from 'react-icons/ti';
import { BiRadioCircleMarked } from 'react-icons/bi';
import { IoAppsOutline, IoEllipsisHorizontalOutline } from 'react-icons/io5';
import { MdLinearScale, MdThumbsUpDown } from 'react-icons/md';

/**
 * Define la configuración, validación y transformación de 
 * datos del constructor de preguntas con iconos y colores temáticos.
 */
export const QUESTION_TYPES = [
  { 
    id: 'TEXT', 
    title: 'Texto', 
    desc: 'Permite escribir una respuesta libre.', 
    icon: MdOutlineTextFields,
    color: '#6539B1' 
  },
  { 
    id: 'NUMBER', 
    title: 'Numérica', 
    desc: 'Permite introducir un valor numérico.', 
    icon: TiSortNumerically,
    color: '#DE3ECE' 
  },
  { 
    id: 'SINGLE_CHOICE', 
    title: 'Opción única', 
    desc: 'Permite seleccionar una sola opción.', 
    icon: BiRadioCircleMarked,
    color: '#39B1AB' 
  },
  { 
    id: 'MULTIPLE_CHOICE', 
    title: 'Opción múltiple', 
    desc: 'Permite seleccionar una o varias opciones.', 
    icon: TiInputChecked,
    color: '#FFBD30' 
  },
  { 
    id: 'SCALE', 
    title: 'Escala', 
    desc: 'Permite valorar algo dentro de un rango numérico.', 
    icon: MdLinearScale,
    color: '#395FB1' 
  },
  { 
    id: 'MATRIX_SCALE', 
    title: 'Matriz de escala', 
    desc: 'Permite valorar varios elementos utilizando una misma escala.', 
    icon: IoAppsOutline,
    color: '#5E9800' 
  },
  { 
    id: 'BOOLEAN', 
    title: 'Dos opciones', 
    desc: 'Permite elegir entre dos respuestas, como Sí o No.', 
    icon: MdThumbsUpDown,
    color: '#FB963C' 
  },
];

export const RANGE_QUESTION_TYPES = ['NUMBER', 'SCALE', 'MATRIX_SCALE'];
export const CHOICE_QUESTION_TYPES = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'];

export const SCALE_DISPLAY_OPTIONS = [
  { value: 'NUMERIC', label: 'Numérica' },
  { value: 'STARS', label: 'Estrellas' },
  { value: 'FACES', label: 'Caras' },
];

export const INITIAL_QUESTION_STATE = {
  catalogName: '',
  statement: '',
  annotation: '',
  questionType: 'TEXT',
  evaluationDirection: 'NEUTRAL',
  evaluationWeight: 1,
  maxLength: 300,
  minValue: 1,
  maxValue: 5,
  step: 1,
  options: [
    { label: 'Opción 1', evaluationFactor: 1 },
    { label: 'Opción 2', evaluationFactor: 0 },
  ],
  hasOtherOption: false,
  minSelections: 1,
  maxSelections: 2,
  matrixRows: [
    { label: 'Fila 1' },
    { label: 'Fila 2' },
  ],
  leftLabel: 'Totalmente en desacuerdo',
  rightLabel: 'Totalmente de acuerdo',
  scaleDisplayType: 'NUMERIC',
  trueLabel: 'Sí',
  falseLabel: 'No',
  trueFactor: 1,
  falseFactor: 0,
  fontFamily: 'Georgia, "Times New Roman", serif',
  fontSize: 16,
  textColor: '#1e293b',
  buttonShape: 'ROUNDED',
};

export function getQuestionBuilderValidationError(data, isCatalog = true) {
  if (isCatalog && !hasText(data.catalogName)) return 'Debes asignar un nombre en el catálogo.';
  if (!hasText(data.statement)) return 'Debes redactar el enunciado de la pregunta.';
  if (!QUESTION_TYPES.some((type) => type.id === data.questionType)) return 'Selecciona un tipo de pregunta válido.';
  if (!['POSITIVE', 'NEGATIVE', 'NEUTRAL'].includes(data.evaluationDirection)) return 'Selecciona una dirección de evaluación válida.';
  if (data.questionType === 'TEXT' && data.evaluationDirection !== 'NEUTRAL') return 'Las preguntas de texto deben ser neutrales.';
  if (data.evaluationDirection !== 'NEUTRAL' && !isPositiveNumber(data.evaluationWeight)) return 'El peso debe ser mayor que cero.';
  if (data.questionType === 'TEXT' && !isPositiveInteger(data.maxLength)) return 'La longitud máxima debe ser mayor que cero.';
  if (RANGE_QUESTION_TYPES.includes(data.questionType)) {
    const rangeError = getRangeValidationError(data);
    if (rangeError) return rangeError;
  }
  if (CHOICE_QUESTION_TYPES.includes(data.questionType)) {
    const options = getFilledOptions(data);
    if (options.length < 2) return 'Debes añadir al menos dos opciones de respuesta.';
    if (data.evaluationDirection !== 'NEUTRAL' && options.some((option) => !isValidNumber(getOptionFactor(option)))) return 'Todas las opciones evaluables necesitan un factor.';
  }
  if (data.questionType === 'MULTIPLE_CHOICE') {
    const optionCount = getFilledOptions(data).length + (data.hasOtherOption ? 1 : 0);
    const minSelections = toIntegerOrNull(data.minSelections);
    const maxSelections = toIntegerOrNull(data.maxSelections);
    if (!minSelections || !maxSelections || minSelections < 1 || maxSelections < minSelections || maxSelections > optionCount) return 'Revisa los límites de selección múltiple.';
  }
  if (data.questionType === 'MATRIX_SCALE' && getFilledMatrixRows(data).length === 0) return 'Debes añadir al menos una fila en la matriz.';
  if (data.questionType === 'BOOLEAN' && (!hasText(data.trueLabel) || !hasText(data.falseLabel))) return 'Debes indicar las dos etiquetas de respuesta.';
  if (data.questionType === 'BOOLEAN' && data.evaluationDirection !== 'NEUTRAL' && (!isValidNumber(data.trueFactor) || !isValidNumber(data.falseFactor))) return 'Las respuestas evaluables necesitan sus factores.';
  return '';
}

export function buildCreateFixedQuestionPayload(data) {
  return {
    catalogName: data.catalogName.trim(),
    question: buildQuestionDraftPayload(data),
  };
}

export function buildQuestionDraftPayload(data) {
  return {
    statement: data.statement.trim(),
    annotation: trimToNull(data.annotation),
    questionType: data.questionType,
    evaluationDirection: data.evaluationDirection,
    evaluationWeight: data.evaluationDirection === 'NEUTRAL' ? null : toNumberOrNull(data.evaluationWeight),
    settings: buildQuestionSettings(data),
    options: buildQuestionOptions(data),
    matrixRows: buildMatrixRows(data),
  };
}

export function getOptionText(option) {
  return String(option?.label ?? option?.text ?? '').trim();
}

export function getOptionFactor(option) {
  return option?.evaluationFactor ?? option?.factor ?? '';
}

export function getMatrixRowText(row) {
  return String(typeof row === 'string' ? row : row?.label ?? '').trim();
}

export function parseNumberValue(value) {
  if (value === '') return '';
  const number = Number.parseFloat(String(value).replace(',', '.'));
  return Number.isNaN(number) ? '' : number;
}

export function parseIntegerValue(value) {
  if (value === '') return '';
  const number = Number.parseInt(value, 10);
  return Number.isNaN(number) ? '' : number;
}

export function getNumberValue(value, fallback) {
  const number = toNumberOrNull(value);
  return number === null ? fallback : number;
}

export function getScaleDisplayValue(data) {
  return normalizeScaleDisplayType(data.scaleDisplayType ?? data.visualScaleType);
}

export function getScaleValues(data) {
  const minValue = getNumberValue(data.minValue, 1);
  const maxValue = getNumberValue(data.maxValue, 5);
  const step = getNumberValue(data.step, 1);
  if (maxValue <= minValue || step <= 0) return [minValue];
  const totalSteps = Math.floor((maxValue - minValue) / step) + 1;
  return Array.from({ length: Math.min(totalSteps, 30) }, (_, index) => Number((minValue + index * step).toFixed(2)));
}

export function getFontSize(value) {
  if (!value) return '16px';
  if (typeof value === 'number') return `${value}px`;
  const fontSize = String(value).trim();
  return /^\d+(\.\d+)?$/.test(fontSize) ? `${fontSize}px` : fontSize;
}

function buildQuestionSettings(data) {
  const commonSettings = {
    fontFamily: trimToNull(data.fontFamily),
    fontSize: toIntegerOrNull(data.fontSize),
    textColor: trimToNull(data.textColor ?? data.fontColor),
    buttonShape: data.buttonShape || null,
  };
  if (data.questionType === 'TEXT') return cleanObject({ ...commonSettings, maxLength: toIntegerOrNull(data.maxLength) });
  if (data.questionType === 'NUMBER') return cleanObject({ ...commonSettings, minValue: toNumberOrNull(data.minValue), maxValue: toNumberOrNull(data.maxValue), step: toNumberOrNull(data.step) });
  if (['SCALE', 'MATRIX_SCALE'].includes(data.questionType)) {
    return cleanObject({
      ...commonSettings,
      minValue: toNumberOrNull(data.minValue),
      maxValue: toNumberOrNull(data.maxValue),
      step: toNumberOrNull(data.step),
      leftLabel: trimToNull(data.leftLabel),
      rightLabel: trimToNull(data.rightLabel),
      scaleDisplayType: getScaleDisplayValue(data),
    });
  }
  if (data.questionType === 'MULTIPLE_CHOICE') return cleanObject({ ...commonSettings, minSelections: toIntegerOrNull(data.minSelections), maxSelections: toIntegerOrNull(data.maxSelections) });
  if (data.questionType === 'BOOLEAN') {
    return cleanObject({
      ...commonSettings,
      trueLabel: trimToNull(data.trueLabel),
      falseLabel: trimToNull(data.falseLabel),
      trueEvaluationFactor: data.evaluationDirection === 'NEUTRAL' ? null : toNumberOrNull(data.trueFactor),
      falseEvaluationFactor: data.evaluationDirection === 'NEUTRAL' ? null : toNumberOrNull(data.falseFactor),
    });
  }
  return cleanObject(commonSettings);
}

function buildQuestionOptions(data) {
  if (!CHOICE_QUESTION_TYPES.includes(data.questionType)) return [];
  const options = getFilledOptions(data).map((option, index) => {
    const payload = {
      label: getOptionText(option),
      position: index + 1,
      other: Boolean(option.other),
    };
    if (data.evaluationDirection !== 'NEUTRAL') payload.evaluationFactor = toNumberOrNull(getOptionFactor(option));
    return payload;
  });
  if (data.hasOtherOption && !options.some((option) => option.other)) {
    const payload = {
      label: 'Otro',
      position: options.length + 1,
      other: true,
    };
    if (data.evaluationDirection !== 'NEUTRAL') payload.evaluationFactor = 0;
    options.push(payload);
  }
  return options;
}

function buildMatrixRows(data) {
  if (data.questionType !== 'MATRIX_SCALE') return [];
  return getFilledMatrixRows(data).map((row, index) => ({
    label: getMatrixRowText(row),
    position: index + 1,
  }));
}

function getFilledOptions(data) {
  return (data.options || []).filter((option) => hasText(getOptionText(option)));
}

function getFilledMatrixRows(data) {
  return (data.matrixRows || []).filter((row) => hasText(getMatrixRowText(row)));
}

function getRangeValidationError(data) {
  const minValue = toNumberOrNull(data.minValue);
  const maxValue = toNumberOrNull(data.maxValue);
  const step = toNumberOrNull(data.step);
  if (!isValidNumber(minValue) || !isValidNumber(maxValue) || !isValidNumber(step)) return 'El rango debe tener mínimo, máximo y paso.';
  if (minValue >= maxValue) return 'El mínimo debe ser menor que el máximo.';
  if (step <= 0) return 'El paso debe ser mayor que cero.';
  if (data.questionType !== 'NUMBER') {
    const scaleDisplayType = getScaleDisplayValue(data);
    if ((scaleDisplayType === 'STARS' || scaleDisplayType === 'FACES') && (minValue !== 1 || step !== 1 || !Number.isInteger(maxValue))) return 'Las escalas con estrellas o caras deben empezar en 1 y avanzar de 1 en 1.';
    if (scaleDisplayType === 'STARS' && maxValue > 10) return 'La escala de estrellas no puede superar 10 valores.';
    if (scaleDisplayType === 'FACES' && maxValue > 5) return 'La escala de caras no puede superar 5 valores.';
  }
  return '';
}

function normalizeScaleDisplayType(value) {
  if (value === 'STARS' || value === 'FACES') return value;
  return 'NUMERIC';
}

function cleanObject(object) {
  return Object.fromEntries(Object.entries(object).filter(([, value]) => value !== undefined && value !== null && value !== ''));
}

function trimToNull(value) {
  return hasText(value) ? String(value).trim() : null;
}

function hasText(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

function toNumberOrNull(value) {
  if (value === null || value === undefined || value === '') return null;
  const number = Number.parseFloat(String(value).replace(',', '.'));
  return Number.isNaN(number) ? null : number;
}

function toIntegerOrNull(value) {
  const number = toNumberOrNull(value);
  return number === null ? null : Math.trunc(number);
}

function isValidNumber(value) {
  return toNumberOrNull(value) !== null;
}

function isPositiveNumber(value) {
  const number = toNumberOrNull(value);
  return number !== null && number > 0;
}

function isPositiveInteger(value) {
  const number = toIntegerOrNull(value);
  return number !== null && number > 0;
}