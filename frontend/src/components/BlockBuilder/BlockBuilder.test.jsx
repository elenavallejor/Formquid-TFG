import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { BlockBuilder } from './BlockBuilder.jsx';
import { INITIAL_BLOCK_STATE } from '../../utils/blockBuilderUtils.js';

function Harness({ initial = INITIAL_BLOCK_STATE }) {
  const [data, setData] = useState(initial);
  return <BlockBuilder data={data} onChange={setData} />;
}

async function chooseElement(user, name) {
  await user.click(screen.getByRole('button', { name: 'Crear elemento' }));
  const selector = screen.getByRole('dialog', { name: 'CREAR ELEMENTO' });
  await user.click(within(selector).getByRole('button', { name: new RegExp(name) }));
}

describe('BlockBuilder', () => {
  it('crea, edita y elimina un texto del bloque', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(screen.getByRole('textbox', { name: /Nombre del bloque/ }), 'Bloque inicial');
    await chooseElement(user, 'Texto');
    let dialog = screen.getByRole('dialog', { name: 'AÑADIR TEXTO' });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('El texto no puede estar vacío.');
    await user.type(within(dialog).getByRole('textbox', { name: /Contenido/ }), 'Introducción');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(screen.getByText('Introducción')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Opciones de texto' }));
    await user.click(screen.getByRole('button', { name: 'Editar texto' }));
    dialog = screen.getByRole('dialog', { name: 'EDITAR TEXTO' });
    const text = within(dialog).getByRole('textbox', { name: /Contenido/ });
    await user.clear(text);
    await user.type(text, 'Texto revisado');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar texto' }));
    expect(screen.getByText('Texto revisado')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Opciones de texto' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar del bloque' }));
    dialog = screen.getByRole('dialog', { name: 'ELIMINAR TEXTO' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));
    expect(screen.queryByText('Texto revisado')).not.toBeInTheDocument();
  });

  it('crea una pregunta propia y cambia su obligatoriedad', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await chooseElement(user, 'Pregunta nueva');
    const dialog = screen.getByRole('dialog', { name: 'Crear pregunta propia' });
    fireEvent.change(within(dialog).getByPlaceholderText('Texto que verá el encuestado.'), { target: { value: '¿Qué opinas?' } });
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));
    expect(screen.getByRole('button', { name: 'Opciones de pregunta' })).toBeVisible();
    expect(screen.getByRole('heading', { name: /¿Qué opinas\?/ })).toBeVisible();
  });

  it('permite reordenar contenido con los botones accesibles', async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ name: 'Bloque', description: '', items: [
      { itemType: 'TEXT', textBody: 'Primero' },
      { itemType: 'TEXT', textBody: 'Segundo' },
    ] }} />);
    await user.click(screen.getByRole('button', { name: 'Mover texto 2 hacia arriba' }));
    const texts = screen.getAllByText(/Primero|Segundo/);
    expect(texts.map((node) => node.textContent)).toEqual(['Segundo', 'Primero']);
  });
});
