import { useRef } from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { useModalFocus } from './useModalFocus.js';

function Dialog({ empty = false, preferred = false, closeOnEscape = true, onClose, returnFocusRef, lockScroll = true }) {
  const dialogRef = useRef(null);
  const initialFocusRef = useRef(null);
  useModalFocus({ isOpen: true, dialogRef, initialFocusRef: preferred ? initialFocusRef : undefined, onClose, closeOnEscape, returnFocusRef, lockScroll });
  return <div role="dialog" ref={dialogRef} tabIndex={-1}>{!empty && <><button ref={initialFocusRef}>Primero</button><button>Último</button><button hidden>Oculto</button><button aria-hidden="true">Invisible</button><div inert><button>Inerte</button></div></>}</div>;
}

describe('useModalFocus', () => {
  it.each([true, false])('elige el foco inicial y limita la navegación: preferido=%s', async (preferred) => {
    vi.spyOn(Element.prototype, 'getClientRects').mockReturnValue([{}]);
    const onClose = vi.fn();
    render(<Dialog preferred={preferred} onClose={onClose} />);
    const first = screen.getByRole('button', { name: 'Primero' });
    const last = screen.getByRole('button', { name: 'Último' });
    await waitFor(() => expect(first).toHaveFocus());
    fireEvent.keyDown(first, { key: 'Tab', shiftKey: true });
    expect(last).toHaveFocus();
    fireEvent.keyDown(last, { key: 'Tab' });
    expect(first).toHaveFocus();
    fireEvent.keyDown(first, { key: 'ArrowRight' });
    expect(first).toHaveFocus();
    const outside = document.createElement('button'); document.body.append(outside);
    outside.focus();
    expect(first).toHaveFocus();
    outside.remove();
    fireEvent.keyDown(first, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledOnce();
  });
  it('conserva el foco en un diálogo sin controles y permite desactivar Escape', async () => {
    const onClose = vi.fn();
    render(<Dialog empty onClose={onClose} closeOnEscape={false} lockScroll={false} />);
    const dialog = screen.getByRole('dialog');
    await waitFor(() => expect(dialog).toHaveFocus());
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(dialog).toHaveFocus();
    fireEvent.keyDown(dialog, { key: 'Escape' });
    expect(onClose).not.toHaveBeenCalled();
  });
  it('devuelve el foco al control solicitado al desmontar', async () => {
    const trigger = document.createElement('button'); document.body.append(trigger);
    const view = render(<Dialog returnFocusRef={{ current: trigger }} />);
    view.unmount();
    await waitFor(() => expect(trigger).toHaveFocus());
    trigger.remove();
  });
});
