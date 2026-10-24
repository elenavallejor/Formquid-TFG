import { describe, expect, it } from 'vitest';

import { lockBodyScroll } from './bodyScrollLock.js';

describe('lockBodyScroll', () => {
  it('restaura el scroll aunque el panel y el modal se cierren en otro orden', () => {
    document.body.style.overflow = 'auto';
    const closePanel = lockBodyScroll();
    const closeModal = lockBodyScroll();

    try {
      closePanel();
      expect(document.body.style.overflow).toBe('hidden');
      closeModal();
      expect(document.body.style.overflow).toBe('auto');
    } finally {
      closePanel();
      closeModal();
      document.body.style.overflow = '';
    }
  });
});
