import { Outlet } from 'react-router-dom';

import { Footer } from '../components/Footer/Footer';
import { Header } from '../components/Header/Header';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { useScrollToTop } from '../hooks/useScrollToTop';

import './app.css';

export function App() {
  useDocumentTitle();
  useScrollToTop();

  return <AppLayout><Outlet /></AppLayout>;
}

export function AppLayout({ children }) {
  return (
    <div className="app-layout">
      <a className="skip-link" href="#main-content">
        Saltar al contenido principal
      </a>
      <Header />
      <div id="main-content" className="app-main-content" tabIndex={-1}>
        {children}
      </div>
      <Footer />
    </div>
  );
}
