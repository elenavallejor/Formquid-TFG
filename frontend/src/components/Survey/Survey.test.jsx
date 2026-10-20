import { render, screen, within } from '@testing-library/react';
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
    await user.type(screen.getByRole('textbox', { name: 'Especifica la respuesta «Otro»' }), 'Centro de salud');
    await user.click(screen.getByRole('button', { name: 'Enviar respuestas' }));
    expect(onSubmit).toHaveBeenCalledWith([{ questionId: 11, selectedOptions: [{ optionId: 31, otherText: 'Centro de salud' }] }]);
  });
});
