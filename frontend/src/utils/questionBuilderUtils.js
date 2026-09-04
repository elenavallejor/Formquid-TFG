import { MdFormatColorText, MdPin, MdRadioButtonChecked, MdCheckBox, MdLinearScale, MdGridOn, MdCompareArrows, MdThumbsUpDown } from 'react-icons/md';

/**
 * Define la configuración base y valores por defecto para los 
 * diferentes tipos de preguntas del constructor de preguntas.
 */
export const QUESTION_TYPES = [
  { id: 'TEXT', title: 'Texto', desc: 'Permite escribir una respuesta libre.', icon: MdFormatColorText },
  { id: 'NUMBER', title: 'Numérica', desc: 'Permite introducir un valor numérico.', icon: MdPin },
  { id: 'SINGLE_CHOICE', title: 'Opción única', desc: 'Permite seleccionar una sola opción.', icon: MdRadioButtonChecked },
  { id: 'MULTIPLE_CHOICE', title: 'Opción múltiple', desc: 'Permite seleccionar una o varias opciones.', icon: MdCheckBox },
  { id: 'SCALE', title: 'Escala', desc: 'Permite valorar algo dentro de un rango numérico.', icon: MdLinearScale },
  { id: 'MATRIX_SCALE', title: 'Matriz de escala', desc: 'Permite valorar varios elementos con una misma escala.', icon: MdGridOn },
  { id: 'BIPOLAR_SCALE', title: 'Escala bipolar', desc: 'Permite valorar algo entre dos conceptos opuestos.', icon: MdCompareArrows },
  { id: 'BOOLEAN', title: 'Dos opciones', desc: 'Permite elegir entre dos respuestas, como Sí o No.', icon: MdThumbsUpDown },
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
    { text: 'Opción 1', factor: 1 },
    { text: 'Opción 2', factor: 0 },
  ],
  hasOtherOption: false,
  minSelections: 1,
  maxSelections: 2,
  
  matrixRows: ['Fila 1', 'Fila 2'],
  
  leftLabel: 'Totalmente en desacuerdo',
  rightLabel: 'Totalmente de acuerdo',
  visualScaleType: 'NUMERIC_DISCRETE',
  
  trueLabel: 'Sí',
  falseLabel: 'No',
  trueFactor: 1,
  falseFactor: 0,
  
  fontFamily: 'Georgia, "Times New Roman", serif',
  fontSize: '16px',
  fontColor: '#1e293b',
  buttonShape: 'ROUNDED',
};