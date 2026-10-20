import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

window.matchMedia ??= (query) => ({
  matches: false,
  media: query,
  addEventListener: () => {},
  removeEventListener: () => {},
});
Element.prototype.scrollIntoView ??= () => {};
window.scrollTo = vi.fn();
HTMLDialogElement.prototype.showModal ??= function showModal() { this.open = true; };
HTMLDialogElement.prototype.close ??= function close() { this.open = false; };
window.ResizeObserver ??= class ResizeObserver {
  observe() {}
  disconnect() {}
};

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.useRealTimers();
});
