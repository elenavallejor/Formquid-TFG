import { Outlet } from 'react-router-dom';

import { Footer } from '../components/Footer/Footer';
import { Header } from '../components/Header/Header';
import { useScrollToTop } from '../hooks/useScrollToTop';

import './app.css';

export function App() {
  useScrollToTop();

  return (
    <div className="app-layout">
      <Header />
        <Outlet />
      <Footer />
    </div>
  );
}