import { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Question } from './Question.jsx';

function Answer({ data, initial = null, ...props }) {
  const [value, setValue] = useState(initial);
  return <><Question data={data} value={value} onChange={setValue} interactive {...props} /><output>{JSON.stringify(value)}</output></>;
}

describe('Question: límites y variantes accesibles', () => {
  it.each([
    [{ minValue: 2 }, 'Valor mínimo: 2'],
    [{ maxValue: 8 }, 'Valor máximo: 8'],
    [{ minValue: 2, maxValue: 8 }, 'Entre 2 y 8'],
  ])('presenta el rango numérico %j', (range, hint) => {
    render(<Answer data={{ questionType: 'NUMBER', statement: 'Edad', ...range }} errorMessage="Revisa el número" />);
    expect(screen.getByText(`(${hint})`)).toBeVisible();
    expect(screen.getByRole('spinbutton')).toHaveAttribute('aria-invalid', 'true');
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '4' } });
    expect(screen.getByRole('status')).toHaveTextContent('"4"');
  });

  it.each([
    [{ minSelections: 1, maxSelections: 2 }, 'Selecciona entre 1 y 2 opciones.'],
    [{ maxSelections: 2 }, 'Selecciona como máximo 2 opciones.'],
    [{ minSelections: 1 }, 'Selecciona al menos 1 opciones.'],
  ])('muestra los límites de selección %j y permite desmarcar', (limits, hint) => {
    render(<Answer data={{ questionType: 'MULTIPLE_CHOICE', options: [{ id: 1, label: 'A' }], ...limits }} />);
    expect(screen.getByText(hint)).toBeVisible();
    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);
    expect(checkbox).toBeChecked();
    fireEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  it('conserva las opciones normales al escribir y retirar Otro', () => {
    render(<Answer initial={['id:1']} data={{ questionType: 'MULTIPLE_CHOICE', hasOtherOption: true, options: [{ id: 1, label: 'A' }] }} errorMessage="Especifica" />);
    const other = screen.getByRole('checkbox', { name: 'Otro' });
    fireEvent.click(other);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Alternativa' } });
    expect(screen.getByRole('status')).toHaveTextContent('Alternativa');
    fireEvent.click(other);
    expect(screen.getByRole('status')).toHaveTextContent('["id:1"]');
    expect(screen.getByRole('checkbox', { name: 'A' })).toBeChecked();
  });

  it.each(['STARS', 'FACES', 'NUMERIC'])('permite responder matrices con visualización %s', (scaleDisplayType) => {
    render(<Answer data={{ questionType: 'MATRIX_SCALE', matrixRows: [{ label: '' }], minValue: 1, maxValue: 3, step: 0.5, scaleDisplayType, leftLabel: 'Bajo', rightLabel: 'Alto', isRequired: true }} errorMessage="Responde" />);
    fireEvent.click(screen.getByRole('radio', { name: 'Fila 1 1.5' }));
    expect(screen.getByRole('radio', { name: 'Fila 1 1.5' })).toBeChecked();
    expect(screen.getByRole('columnheader', { name: '1 Bajo' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: '3 Alto' })).toBeVisible();
  });

  it('usa el enunciado por defecto y omite el número en un catálogo', () => {
    render(<Question data={{ questionType: 'BOOLEAN', isRequired: true, trueLabel: 'Acepto', falseLabel: 'Rechazo' }} questionNumber="" headingLevel="incorrecto" isCatalog />);
    expect(screen.getByRole('heading', { level: 3 })).toHaveTextContent('Enunciado de la pregunta...');
    expect(screen.getByRole('radio', { name: 'Acepto' })).toBeDisabled();
    expect(screen.queryByText('(obligatoria)')).not.toBeInTheDocument();
  });
});


it('permite enfocar y desplazar una matriz de solo lectura con teclado', () => {
  render(<Question data={{ questionType: 'MATRIX_SCALE', statement: 'Valoración de servicios', matrixRows: [{ id: 1, label: 'Facilidad' }], minValue: 1, maxValue: 5 }} />);
  const matrix = screen.getByRole('region', { name: 'Matriz de respuestas: Valoración de servicios' });
  expect(matrix).toHaveAttribute('tabindex', '0');
  matrix.focus();
  expect(matrix).toHaveFocus();
  expect(matrix).toContainElement(screen.getByRole('table'));
});
