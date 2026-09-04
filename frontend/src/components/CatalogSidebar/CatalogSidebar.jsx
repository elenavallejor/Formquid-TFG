import { useEffect, useState } from 'react';
import { MdChevronLeft, MdChevronRight, MdClose, MdMenuBook } from 'react-icons/md';
import { getCatalogBlocks, getCatalogQuestions, getCatalogTemplates } from '../../../api/catalogApi.js';
import './CatalogSidebar.css';

/**
 * Panel lateral deslizante y paginado que permite consultar 
 * elementos existentes del catálogo.
 */
export function CatalogSidebar({ showQuestions = true, showBlocks = false, showTemplates = false }) {
  const [isOpen, setIsOpen] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [pages, setPages] = useState({ q: 0, b: 0, t: 0 });
  const [totals, setTotals] = useState({ q: 0, b: 0, t: 0 });

  useEffect(() => {
    if (!isOpen) return;
    if (showQuestions && questions.length === 0) loadQuestions(0);
    if (showBlocks && blocks.length === 0) loadBlocks(0);
    if (showTemplates && templates.length === 0) loadTemplates(0);
  }, [isOpen]);

  const loadQuestions = async (page) => {
    try {
      const res = await getCatalogQuestions({ page, size: 10 });
      setQuestions(page === 0 ? res.content : [...questions, ...res.content]);
      setTotals((prev) => ({ ...prev, q: res.totalElements }));
      setPages((prev) => ({ ...prev, q: page }));
    } catch (e) {
      console.error(e);
    }
  };

  const loadBlocks = async (page) => {
    try {
      const res = await getCatalogBlocks({ page, size: 10 });
      setBlocks(page === 0 ? res.content : [...blocks, ...res.content]);
      setTotals((prev) => ({ ...prev, b: res.totalElements }));
      setPages((prev) => ({ ...prev, b: page }));
    } catch (e) {
      console.error(e);
    }
  };

  const loadTemplates = async (page) => {
    try {
      const res = await getCatalogTemplates({ page, size: 10 });
      setTemplates(page === 0 ? res.content : [...templates, ...res.content]);
      setTotals((prev) => ({ ...prev, t: res.totalElements }));
      setPages((prev) => ({ ...prev, t: page }));
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <>
      <button
        type="button"
        className={`sidebar-dock-btn ${isOpen ? 'hidden' : ''}`}
        onClick={() => setIsOpen(true)}
        aria-label="Abrir catálogo existente"
      >
        <MdMenuBook />
        <span>Ver Catálogo</span>
      </button>

      <aside className={`catalog-sidebar-drawer ${isOpen ? 'open' : ''}`}>
        <header className="sidebar-header">
          <h3>Elementos existentes</h3>
          <button type="button" onClick={() => setIsOpen(false)} aria-label="Cerrar barra lateral">
            <MdClose />
          </button>
        </header>

        <div className="sidebar-scrollable-content">
          {showQuestions && (
            <section className="sidebar-group">
              <h4>Preguntas fijas ({totals.q})</h4>
              <ul>
                {questions.map((q) => (
                  <li key={q.id}>
                    <strong>{q.catalogName || q.statement}</strong>
                    <span>{q.questionType}</span>
                  </li>
                ))}
              </ul>
              {questions.length < totals.q && (
                <button type="button" className="btn-load-more" onClick={() => loadQuestions(pages.q + 1)}>
                  Mostrar más
                </button>
              )}
            </section>
          )}

          {showBlocks && (
            <section className="sidebar-group">
              <h4>Bloques ({totals.b})</h4>
              <ul>
                {blocks.map((b) => (
                  <li key={b.id}>
                    <strong>{b.name}</strong>
                    <span>{b.questionCount || 0} preguntas</span>
                  </li>
                ))}
              </ul>
              {blocks.length < totals.b && (
                <button type="button" className="btn-load-more" onClick={() => loadBlocks(pages.b + 1)}>
                  Mostrar más
                </button>
              )}
            </section>
          )}

          {showTemplates && (
            <section className="sidebar-group">
              <h4>Plantillas ({totals.t})</h4>
              <ul>
                {templates.map((t) => (
                  <li key={t.id}>
                    <strong>{t.title}</strong>
                    <span>{t.questionCount || 0} preguntas</span>
                  </li>
                ))}
              </ul>
              {templates.length < totals.t && (
                <button type="button" className="btn-load-more" onClick={() => loadTemplates(pages.t + 1)}>
                  Mostrar más
                </button>
              )}
            </section>
          )}
        </div>
      </aside>
    </>
  );
}