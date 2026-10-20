import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { QuestionBuilder } from './QuestionBuilder.jsx';
import { INITIAL_QUESTION_STATE } from '../../utils/questionBuilderUtils.js';

function Harness({ initial = INITIAL_QUESTION_STATE, isModal = false, onSave = vi.fn(), includeEvaluation = false }) {
  const [data, setData] = useState(initial);
  return (
    <QuestionBuilder
      data={data}
      onChange={setData}
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
});
