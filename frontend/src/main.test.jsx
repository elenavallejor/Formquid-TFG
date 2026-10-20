import { afterEach, describe, expect, it, vi } from 'vitest';

const render = vi.fn();
const createRoot = vi.fn(() => ({ render }));
vi.mock('react-dom/client', () => ({ createRoot }));
vi.mock('./app/provider', () => ({ AppProvider: () => <div>Aplicación</div> }));

describe('arranque del frontend', () => {
  afterEach(() => {
    document.body.innerHTML = '';
    vi.resetModules();
    vi.clearAllMocks();
  });

  it('monta la aplicación en el root y restablece el desplazamiento', async () => {
    document.body.innerHTML = '<div id="root"></div>';
    await import('./main.jsx');
    expect(createRoot).toHaveBeenCalledWith(document.getElementById('root'));
    expect(render).toHaveBeenCalledOnce();
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
  });

  it('avisa de forma explícita si falta el punto de montaje', async () => {
    await expect(import('./main.jsx')).rejects.toThrow('No se ha encontrado el root');
  });
});
