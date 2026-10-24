import { useEffect, useRef } from 'react';
import { lockBodyScroll } from '../utils/bodyScrollLock.js';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'area[href]',
  'button:not(:disabled)',
  'input:not(:disabled):not([type="hidden"])',
  'select:not(:disabled)',
  'textarea:not(:disabled)',
  'summary',
  '[contenteditable="true"]',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

const modalStack = [];

function getFocusableElements(container) {
  if (!container) return [];
  return Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR)).filter((element) => (
    !element.hidden
    && element.getAttribute('aria-hidden') !== 'true'
    && !element.closest('[inert]')
    && element.getClientRects().length > 0
  ));
}

function getInitialFocusTarget(dialog, initialFocusRef) {
  const preferredTarget = initialFocusRef?.current;
  if (
    preferredTarget
    && dialog.contains(preferredTarget)
    && !preferredTarget.matches(':disabled')
    && preferredTarget.getClientRects().length > 0
  ) {
    return preferredTarget;
  }
  return getFocusableElements(dialog)[0] || dialog;
}

/**
 * Aplica el patrón de teclado de WAI-ARIA a un diálogo modal.
 */
export function useModalFocus({
  isOpen,
  onClose,
  dialogRef,
  initialFocusRef,
  returnFocusRef,
  closeOnEscape = true,
  lockScroll = true,
}) {
  const modalIdRef = useRef(Symbol('modal'));
  const onCloseRef = useRef(onClose);
  const closeOnEscapeRef = useRef(closeOnEscape);
  const shouldRestoreFocusRef = useRef(true);

  useEffect(() => {
    onCloseRef.current = onClose;
    closeOnEscapeRef.current = closeOnEscape;
  }, [closeOnEscape, onClose]);

  useEffect(() => {
    if (!isOpen) return undefined;

    const modalId = modalIdRef.current;
    const previouslyFocused = document.activeElement;
    const requestedReturnFocus = returnFocusRef?.current;
    shouldRestoreFocusRef.current = true;
    modalStack.push(modalId);
    const unlockScroll = lockScroll ? lockBodyScroll() : null;

    function isTopModal() {
      return modalStack[modalStack.length - 1] === modalId;
    }

    function focusInitialElement() {
      const dialog = dialogRef.current;
      if (!dialog || !isTopModal()) return;
      getInitialFocusTarget(dialog, initialFocusRef).focus({ preventScroll: true });
    }

    const focusFrame = window.requestAnimationFrame(focusInitialElement);

    function handleKeyDown(event) {
      const dialog = dialogRef.current;
      if (!dialog || !isTopModal()) return;

      if (event.key === 'Escape' && closeOnEscapeRef.current) {
        event.preventDefault();
        event.stopPropagation();
        onCloseRef.current?.();
        return;
      }

      if (event.key !== 'Tab') return;
      const focusableElements = getFocusableElements(dialog);
      if (focusableElements.length === 0) {
        event.preventDefault();
        dialog.focus({ preventScroll: true });
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      const activeElement = document.activeElement;

      if (event.shiftKey && (activeElement === firstElement || !dialog.contains(activeElement))) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && (activeElement === lastElement || !dialog.contains(activeElement))) {
        event.preventDefault();
        firstElement.focus();
      }
    }

    function handleFocusIn(event) {
      const dialog = dialogRef.current;
      if (!dialog || !isTopModal() || dialog.contains(event.target)) return;
      getInitialFocusTarget(dialog, initialFocusRef).focus({ preventScroll: true });
    }

    document.addEventListener('keydown', handleKeyDown, true);
    document.addEventListener('focusin', handleFocusIn, true);

    return () => {
      window.cancelAnimationFrame(focusFrame);
      document.removeEventListener('keydown', handleKeyDown, true);
      document.removeEventListener('focusin', handleFocusIn, true);
      const stackIndex = modalStack.lastIndexOf(modalId);
      if (stackIndex >= 0) modalStack.splice(stackIndex, 1);
      unlockScroll?.();

      const focusTarget = requestedReturnFocus || previouslyFocused;
      if (shouldRestoreFocusRef.current && focusTarget?.isConnected) {
        window.requestAnimationFrame(() => focusTarget.focus({ preventScroll: true }));
      }
    };
  }, [dialogRef, initialFocusRef, isOpen, lockScroll, returnFocusRef]);

  function preventFocusRestore() {
    shouldRestoreFocusRef.current = false;
  }

  return { preventFocusRestore };
}
