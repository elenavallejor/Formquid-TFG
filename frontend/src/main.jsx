import * as React from 'react';
import { createRoot } from 'react-dom/client';

import './assets/styles/global.css';
import { AppProvider } from './app/provider';

const root = document.getElementById('root');

if (!root) {
  throw new Error('No se ha encontrado el root');
}

if ('scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

window.scrollTo(0, 0);

createRoot(root).render(
  <React.StrictMode>
    <AppProvider />
  </React.StrictMode>,
);