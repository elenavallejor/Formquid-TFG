import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { QuestionBuilder } from './QuestionBuilder.jsx';
import { INITIAL_QUESTION_STATE } from '../../utils/questionBuilderUtils.js';

function Harness({ initial = INITIAL_QUESTION_STATE, isModal = false, onSave = vi.fn(), onDataChange, includeEvaluation = false }) {
  const [data, setData] = useState(initial);
  return (
    <QuestionBuilder
      data={data}
      onChange={(nextData) => { setData(nextData); onDataChange?.(nextData); }}
      isCatalog={false}
      isModal={isModal}
      onClose={vi.fn()}
      onSave={onSave}
      includeEvaluation={includeEvaluation}
    />
  );
}

describe('QuestionBuilder', () => {
  it('impide guardar una pregunta sin enunciado y permite corregirla', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<Harness isModal onSave={onSave} />);
    const dialog = screen.getByRole('dialog', { name: 'Configurar pregunta' });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('Debes redactar el enunciado');
    await user.type(within(dialog).getByPlaceholderText('Texto que verá el encuestado.'), '¿Qué opinas?');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));
    expect(onSave).toHaveBeenCalledOnce();
  });

  it('permite crear, editar y eliminar opciones de respuesta', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: /Opción única/ }));
    await user.click(screen.getByRole('button', { name: 'Añadir opción' }));
    const options = screen.getAllByPlaceholderText('Texto de la opción');
    expect(options).toHaveLength(3);
    await user.clear(options[2]);
    await user.type(options[2], 'Otra alternativa');
    expect(screen.getByDisplayValue('Otra alternativa')).toBeVisible();
    await user.click(screen.getAllByRole('button', { name: 'Eliminar opción' })[0]);
    expect(screen.getAllByPlaceholderText('Texto de la opción')).toHaveLength(2);
    await user.click(screen.getByRole('checkbox', { name: /Permitir opción/ }));
    expect(screen.getByRole('checkbox', { name: /Permitir opción/ })).toBeChecked();
  });

  it('ajusta una escala visual y permite añadir filas de matriz', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: /Matriz de escala/ }));
    await user.click(screen.getByRole('button', { name: 'Añadir fila' }));
    expect(screen.getAllByPlaceholderText('Texto de la fila')).toHaveLength(3);
    await user.click(screen.getAllByRole('button', { name: 'Eliminar fila' })[0]);
    expect(screen.getAllByPlaceholderText('Texto de la fila')).toHaveLength(2);
    await user.click(screen.getByRole('button', { name: /Tipo visual de la escala/ }));
    fireEvent.click(within(screen.getByRole('group', { name: 'Tipo visual de la escala' })).getByRole('button', { name: 'Estrellas' }));
    expect(screen.getByRole('button', { name: /Tipo visual de la escala: Estrellas/ })).toBeVisible();
  });

  it('configura una pregunta booleana con evaluación', async () => {
    const user = userEvent.setup();
    render(<Harness includeEvaluation />);
    await user.click(screen.getByRole('button', { name: /Dos opciones/ }));
    await user.click(screen.getByRole('button', { name: 'Positiva' }));
    expect(screen.getByRole('button', { name: 'Positiva' })).toHaveAttribute('aria-pressed', 'true');
    await user.clear(screen.getByPlaceholderText('Etiqueta afirmativa'));
    await user.type(screen.getByPlaceholderText('Etiqueta afirmativa'), 'De acuerdo');
    expect(screen.getByDisplayValue('De acuerdo')).toBeVisible();
  });

  it('permite introducir el peso manualmente y muestra el signo de la dirección', async () => {
    const user = userEvent.setup();
    const onDataChange = vi.fn();
    render(<Harness includeEvaluation onDataChange={onDataChange} />);
    await user.click(screen.getByRole('button', { name: /Dos opciones/ }));
    await user.click(screen.getByRole('button', { name: 'Positiva' }));
    const weight = screen.getByRole('textbox', { name: 'Peso de la pregunta:' });
    expect(weight).toHaveValue('');
    fireEvent.change(weight, { target: { value: '3.5' } });
    expect(weight).toHaveValue('3.5');
    await user.click(screen.getByText('?', { selector: 'summary' }));
    expect(screen.getByText(/métricas de la investigación/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Negativa' }));
    expect(weight).toHaveValue('-3.5');
    expect(onDataChange.mock.lastCall[0].evaluationWeight).toBe(-3.5);
    fireEvent.change(weight, { target: { value: '-4.5' } });
    expect(weight).toHaveValue('-4.5');
    expect(onDataChange.mock.lastCall[0].evaluationWeight).toBe('-4.5');
    await user.click(screen.getByRole('button', { name: 'Positiva' }));
    expect(weight).toHaveValue('4.5');
    expect(onDataChange.mock.lastCall[0].evaluationWeight).toBe(4.5);
  });
});


describe('QuestionBuilder: campos de configuración', () => {
  it.each(['NUMBER', 'SCALE', 'MATRIX_SCALE', 'MULTIPLE_CHOICE', 'BOOLEAN', 'TEXT'])('actualiza los campos de %s', (questionType) => {
    const onDataChange = vi.fn();
    render(<Harness initial={{ ...INITIAL_QUESTION_STATE, questionType, statement: 'Pregunta', evaluationDirection: 'POSITIVE', evaluationWeight: 2 }} includeEvaluation onDataChange={onDataChange} />);
    const fields = screen.queryAllByRole('spinbutton').filter(node => !node.disabled);
    fields.forEach((field, index) => fireEvent.change(field, { target: { value: String(index + 2) } }));
    for (const placeholder of ['Texto de la fila', 'Etiqueta afirmativa', 'Etiqueta negativa', 'Ej. Totalmente en desacuerdo', 'Ej. Totalmente de acuerdo']) {
      screen.queryAllByPlaceholderText(placeholder).forEach(field => fireEvent.change(field, { target: { value: 'Modificado' } }));
    }
    fireEvent.click(screen.getByRole('checkbox', { name: 'Respuesta obligatoria para el encuestado' }));
    fireEvent.change(screen.getByPlaceholderText('Agrega una nota o contexto adicional para la pregunta...'), { target: { value: 'Nota' } });
    expect(onDataChange.mock.lastCall[0]).toMatchObject({ annotation: 'Nota', isRequired: true });
  });
  it.each(['', null, undefined])('inicializa factores vacíos y vuelve a neutral: %s', (factor) => {
    const onDataChange = vi.fn();
    render(<Harness initial={{ ...INITIAL_QUESTION_STATE, questionType: 'MULTIPLE_CHOICE', trueFactor: factor, falseFactor: factor, options: [{ label: 'A' }, { label: 'B' }], evaluationWeight: '' }} includeEvaluation onDataChange={onDataChange} />);
    fireEvent.click(screen.getByRole('button', { name: 'Positiva' }));
    expect(onDataChange.mock.lastCall[0]).toMatchObject({ trueFactor: 1, falseFactor: 0 });
    fireEvent.click(screen.getByRole('button', { name: 'Añadir opción' }));
    expect(onDataChange.mock.lastCall[0].options.at(-1).evaluationFactor).toBe(0);
    fireEvent.click(screen.getByRole('button', { name: 'Neutral' }));
    expect(onDataChange.mock.lastCall[0].evaluationWeight).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: /^Texto/ }));
    expect(onDataChange.mock.lastCall[0].questionType).toBe('TEXT');
  });
  it('edita filas heredadas como texto y añade filas a una matriz vacía', () => {
    render(<Harness initial={{ ...INITIAL_QUESTION_STATE, questionType: 'MATRIX_SCALE', matrixRows: ['Original'] }} />);
    fireEvent.change(screen.getByPlaceholderText('Texto de la fila'), { target: { value: 'Nueva fila' } });
    expect(screen.getByDisplayValue('Nueva fila')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar fila' }));
    fireEvent.click(screen.getByRole('button', { name: 'Añadir fila' }));
    expect(screen.getByDisplayValue('Fila 1')).toBeVisible();
  });
});


describe('QuestionBuilder: nombres accesibles', () => {
  it.each(['TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN'])(
    'identifica los campos de configuración de %s sin depender del placeholder', (questionType) => {
      render(<Harness initial={{ ...INITIAL_QUESTION_STATE, questionType, statement: 'Pregunta', evaluationDirection: 'POSITIVE', evaluationWeight: 2 }} includeEvaluation />);
      for (const role of ['textbox', 'spinbutton']) {
        for (const control of screen.queryAllByRole(role)) expect(control).toHaveAccessibleName();
      }
    },
  );

  it('asocia cada etiqueta al editor correspondiente cuando hay varias preguntas', async () => {
    const user = userEvent.setup();
    const { container } = render(<><Harness /><Harness /></>);
    const labels = [...container.querySelectorAll('.builder-field label')].filter((label) => label.textContent.startsWith('Enunciado'));
    const controls = labels.map((label) => document.getElementById(label.htmlFor));
    expect(new Set(controls).size).toBe(2);
    await user.click(labels[1]);
    expect(controls[1]).toHaveFocus();
    await user.type(controls[1], 'Segunda pregunta');
    expect(controls[0]).toHaveValue('');
    expect(controls[1]).toHaveValue('Segunda pregunta');
  });
});
