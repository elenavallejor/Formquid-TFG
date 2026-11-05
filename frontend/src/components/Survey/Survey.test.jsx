import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Survey } from './Survey.jsx';
import { SURVEY_MODES } from './SurveyModes.js';

function question(id, questionType, statement, extra = {}) {
  return { itemType: 'QUESTION', id, questionType, statement, ...extra };
}

describe('Survey', () => {
  it('exige las respuestas obligatorias y construye los datos de texto y número', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{
      title: 'Estudio de salud',
      items: [
        question(1, 'TEXT', 'Nombre', { isRequired: true }),
        question(2, 'NUMBER', 'Edad', { isRequired: true, minValue: 18, maxValue: 90 }),
      ],
    }} />);
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getAllByRole('alert')).toHaveLength(2);
    expect(onSubmit).not.toHaveBeenCalled();
    await user.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Ana');
    await user.type(screen.getByRole('spinbutton', { name: 'Edad' }), '17');
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('El valor mínimo es 18.');
    await user.clear(screen.getByRole('spinbutton', { name: 'Edad' }));
    await user.type(screen.getByRole('spinbutton', { name: 'Edad' }), '24');
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([
      { questionId: 1, textValue: 'Ana' },
      { questionId: 2, numericValue: 24 },
    ]);
  });

  it('envía opciones simples, múltiples y booleanas con sus identificadores', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [
      question(3, 'SINGLE_CHOICE', 'Turno', { options: [{ id: 10, label: 'Mañana' }, { id: 11, label: 'Tarde' }] }),
      question(4, 'MULTIPLE_CHOICE', 'Aficiones', { options: [{ id: 12, label: 'Leer' }, { id: 13, label: 'Andar' }], minSelections: 2 }),
      question(5, 'BOOLEAN', '¿Aceptas?', { isRequired: true }),
    ] }} />);
    await user.click(screen.getByRole('radio', { name: 'Mañana' }));
    await user.click(screen.getByRole('checkbox', { name: 'Leer' }));
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getAllByRole('alert')).toHaveLength(2);
    await user.click(screen.getByRole('checkbox', { name: 'Andar' }));
    await user.click(screen.getByRole('radio', { name: 'Sí' }));
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([
      { questionId: 3, selectedOptions: [{ optionId: 10 }] },
      { questionId: 4, selectedOptions: [{ optionId: 12 }, { optionId: 13 }] },
      { questionId: 5, booleanValue: true },
    ]);
  });

  it('incluye preguntas dentro de bloques y permite cerrar la previsualización', async () => {
    const user = userEvent.setup();
    const onClosePreview = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} isPreview onClosePreview={onClosePreview} data={{
      title: 'Encuesta de prueba',
      items: [
        { itemType: 'TEXT', textBody: 'Introducción' },
        { itemType: 'BLOCK', block: { name: 'Sección A', contentItems: [
          { itemType: 'TEXT', textBody: 'Contexto' },
          question(7, 'TEXT', 'Comentario'),
        ] } },
      ],
    }} />);
    expect(screen.getByText('Introducción')).toBeVisible();
    expect(screen.getByText('Contexto')).toBeVisible();
    expect(screen.getByRole('heading', { name: /Sección A/ })).toBeVisible();
    expect(screen.getByRole('textbox', { name: 'Comentario' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'Cerrar previsualización' }));
    expect(onClosePreview).toHaveBeenCalledOnce();
  });

  it('muestra la encuesta en modo lectura con respuestas deshabilitadas', () => {
    render(<Survey data={{ title: 'Encuesta', items: [question(8, 'TEXT', 'Respuesta')] }} />);
    expect(screen.getByRole('textbox', { name: 'Respuesta' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Enviar respuestas' })).not.toBeInTheDocument();
  });

  it('recoge una escala visual y todas las filas de una matriz', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [
      question(9, 'SCALE', 'Satisfacción', { minValue: 1, maxValue: 3, scaleDisplayType: 'STARS', isRequired: true }),
      question(10, 'MATRIX_SCALE', 'Aspectos', { minValue: 1, maxValue: 3, isRequired: true, matrixRows: [
        { id: 21, label: 'Facilidad' }, { id: 22, label: 'Utilidad' },
      ] }),
    ] }} />);
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getAllByRole('alert')).toHaveLength(2);
    await user.click(within(screen.getByRole('group', { name: /Satisfacción/ })).getByRole('radio', { name: 'Valor 2' }));
    await user.click(within(screen.getByRole('row', { name: /Facilidad/ })).getByRole('radio', { name: 'Facilidad 3' }));
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Responde todas las filas');
    await user.click(within(screen.getByRole('row', { name: /Utilidad/ })).getByRole('radio', { name: 'Utilidad 1' }));
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([
      { questionId: 9, numericValue: 2 },
      { questionId: 10, matrixValues: [{ matrixRowId: 21, numericValue: 3 }, { matrixRowId: 22, numericValue: 1 }] },
    ]);
  });

  it('exige precisar la respuesta Otro y envía su texto', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [
      question(11, 'SINGLE_CHOICE', 'Origen', { options: [{ id: 30, label: 'Internet' }, { id: 31, label: 'Otro', other: true }] }),
    ] }} />);
    await user.click(screen.getByRole('radio', { name: 'Otro' }));
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Especifica la respuesta «Otro».');
    await waitFor(() => expect(screen.getByRole('group', { name: /Origen/ })).toHaveFocus());
    await user.type(screen.getByRole('textbox', { name: 'Especifica la respuesta «Otro»' }), 'Centro de salud');
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([{ questionId: 11, selectedOptions: [{ optionId: 31, otherText: 'Centro de salud' }] }]);
  });
});


describe('Survey: respuestas opcionales y restricciones', () => {
  it('omite respuestas opcionales vacías de todos los tipos', () => {
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: ['TEXT', 'NUMBER', 'BOOLEAN', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'CUSTOM'].map((type, i) => question(i + 1, type, type)) }} />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([]);
  });
  it.each([
    [{ questionType: 'NUMBER', maxValue: 10 }, '12', 'El valor máximo es 10.'],
    [{ questionType: 'NUMBER', step: 2 }, '3', 'Usa incrementos de 2.'],
    [{ questionType: 'TEXT', settings: { maxLength: 3 } }, 'largo', 'La respuesta no puede superar 3 caracteres.'],
  ])('valida restricciones de respuesta %j', (settings, value, error) => {
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [question(1, settings.questionType, 'Respuesta', settings)] }} />);
    fireEvent.change(screen.getByRole(settings.questionType === 'TEXT' ? 'textbox' : 'spinbutton'), { target: { value } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent(error);
    expect(onSubmit).not.toHaveBeenCalled();
  });
  it.each([1, 2])('exige el mínimo de %i selecciones obligatorias', (minSelections) => {
    render(<Survey mode={SURVEY_MODES.RESPOND} data={{ items: [question(1, 'MULTIPLE_CHOICE', 'Selección', { isRequired: true, minSelections, options: [{ id: 1, label: 'A' }, { id: 2, label: 'B' }] })] }} />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Esta pregunta es obligatoria');
  });
  it('limita las selecciones múltiples y permite corregirlas', () => {
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [question(1, 'MULTIPLE_CHOICE', 'Selección', { maxSelections: 1, options: [{ id: 1, label: 'A' }, { id: 2, label: 'B' }] })] }} />);
    fireEvent.click(screen.getByRole('checkbox', { name: 'A' }));
    fireEvent.click(screen.getByRole('checkbox', { name: 'B' }));
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona como máximo 1 opciones.');
    fireEvent.click(screen.getByRole('checkbox', { name: 'B' }));
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([{ questionId: 1, selectedOptions: [{ optionId: 1 }] }]);
  });
  it('exige texto para Otro en selecciones múltiples', () => {
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ items: [question(1, 'MULTIPLE_CHOICE', 'Selección', { options: [{ id: 3, label: 'Otro', other: true }] })] }} />);
    fireEvent.click(screen.getByRole('checkbox', { name: 'Otro' }));
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Especifica');
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '  Alternativa  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([{ questionId: 1, selectedOptions: [{ optionId: 3, otherText: 'Alternativa' }] }]);
  });
  it('muestra metadatos y preguntas anidadas de la respuesta del servidor', () => {
    const onSubmit = vi.fn();
    render(<Survey mode={SURVEY_MODES.RESPOND} onSubmit={onSubmit} data={{ fontFamily: 'OPEN_DYSLEXIC', imageKey: 'cover.jpg', description: 'Descripción', tags: ['salud'], contentItems: [
      { itemType: 'QUESTION', question: { id: 1, questionType: 'BOOLEAN', statement: 'Aceptación', options: [], matrixRows: [] } },
      { itemType: 'BLOCK', items: [{ itemType: 'TEXT', textBody: 'Contexto' }, question(null, 'TEXT', 'Sin identificador')] },
      { itemType: 'UNKNOWN' },
    ] }} />);
    expect(screen.getByText('#salud')).toBeVisible();
    expect(screen.getByText('Descripción')).toBeVisible();
    expect(screen.getByText('Sección sin título')).toBeVisible();
    fireEvent.click(screen.getByRole('radio', { name: 'No' }));
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'No se serializa' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([{ questionId: 1, booleanValue: false }]);
  });
  it('muestra el estado vacío y el envío en curso', () => {
    render(<Survey mode={SURVEY_MODES.RESPOND} isSubmitting />);
    expect(screen.getByText('Esta encuesta todavía no contiene elementos.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Enviando...' })).toBeDisabled();
  });
});
