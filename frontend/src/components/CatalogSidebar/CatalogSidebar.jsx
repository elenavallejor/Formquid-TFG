import { useEffect, useState } from 'react';
import { MdChevronLeft, MdChevronRight, MdSearch, MdAdd, MdOutlineVisibility, MdKeyboardArrowDown } from 'react-icons/md';
import { getCatalogBlocks, getCatalogQuestions } from '../../api/catalogApi.js';
import { Skeleton } from '../../components/Skeleton/Skeleton.jsx';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';

import './CatalogSidebar.css';

/**
 * Panel lateral deslizante y paginado situado a la izquierda que permite 
 * consultar elementos existentes del catálogo con el diseño del mockup.
 */
export function CatalogSidebar({ 
  showQuestions = true, 
  showBlocks = true, 
  onAddQuestion, 
  onAddBlock, 
  onPreviewBlock 
}) {
  const [isOpen, setIsOpen] = useState(true);

  const [questions, setQuestions] = useState([]);
  const [blocks, setBlocks] = useState([]);

  const [questionSearch, setQuestionSearch] = useState('');
  const [blockSearch, setBlockSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  const [isLoadingQuestions, setIsLoadingQuestions] = useState(false);
  const [isLoadingBlocks, setIsLoadingBlocks] = useState(false);

  const [pages, setPages] = useState({ q: 0, b: 0 });
  const [totals, setTotals] = useState({ q: 0, b: 0 });

  useEffect(() => {
    if (!isOpen) return;
    if (showQuestions && questions.length === 0) loadQuestions(0);
    if (showBlocks && blocks.length === 0) loadBlocks(0);
  }, [isOpen]);

  const loadQuestions = async (page) => {
    try {
      setIsLoadingQuestions(true);
      const res = await getCatalogQuestions({ page, size: 10 });
      setQuestions((prev) => (page === 0 ? res.content : [...prev, ...res.content]));
      setTotals((prev) => ({ ...prev, q: res.totalElements }));
      setPages((prev) => ({ ...prev, q: page }));
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoadingQuestions(false);
    }
  };

  const loadBlocks = async (page) => {
    try {
      setIsLoadingBlocks(true);
      const res = await getCatalogBlocks({ page, size: 10 });
      setBlocks((prev) => (page === 0 ? res.content : [...prev, ...res.content]));
      setTotals((prev) => ({ ...prev, b: res.totalElements }));
      setPages((prev) => ({ ...prev, b: page }));
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoadingBlocks(false);
    }
  };

  const filteredQuestions = questions.filter((q) => {
    const matchesSearch = (q.statement || q.catalogName || '').toLowerCase().includes(questionSearch.toLowerCase());
    const matchesType = !typeFilter || q.questionType === typeFilter;
    return matchesSearch && matchesType;
  });

  const filteredBlocks = blocks.filter((b) => {
    return (b.name || '').toLowerCase().includes(blockSearch.toLowerCase());
  });

  return (
    <>
      <button
        type="button"
        className={`sidebar-dock-btn left ${isOpen ? 'hidden' : ''}`}
        onClick={() => setIsOpen(true)}
        aria-label="Abrir catálogo"
      >
        <MdChevronRight />
      </button>

      <aside className={`catalog-sidebar-drawer left ${isOpen ? 'open' : ''}`}>
        <header className="sidebar-header-green">
          <h2>CATÁLOGO DE ELEMENTOS</h2>
          <button 
            type="button" 
            className="btn-toggle-drawer" 
            onClick={() => setIsOpen(false)} 
            aria-label="Ocultar catálogo"
          >
            <MdChevronLeft />
          </button>
        </header>

        <div className="sidebar-scrollable-content">
          {showQuestions && (
            <section className="sidebar-section">
              <h3 className="sidebar-section-title">PREGUNTAS FIJAS:</h3>

              <div className="sidebar-filter-row">
                <div className="sidebar-search-wrapper">
                  <MdSearch className="search-icon-inside" />
                  <input
                    type="text"
                    placeholder="Buscar preguntas..."
                    value={questionSearch}
                    onChange={(e) => setQuestionSearch(e.target.value)}
                  />
                </div>
                <div className="sidebar-select-wrapper">
                  <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
                    <option value="">Filtrar por tipo</option>
                    {QUESTION_TYPES.map((t) => (
                      <option key={t.id} value={t.id}>{t.title}</option>
                    ))}
                  </select>
                  <MdKeyboardArrowDown className="select-arrow-icon" />
                </div>
              </div>

              {isLoadingQuestions && questions.length === 0 ? (
                <Skeleton variant="list" count={4} hasActions={false} />
              ) : (
                <div className="sidebar-cards-list">
                  {filteredQuestions.map((q) => {
                    const typeConfig = QUESTION_TYPES.find((t) => t.id === q.questionType) || QUESTION_TYPES[0];
                    const Icon = typeConfig.icon;
                    return (
                      <article key={q.id} className="sidebar-item-card">
                        <span 
                          className="item-icon-box"
                          style={{ color: typeConfig.color, borderColor: typeConfig.color }}
                        >
                          <Icon />
                        </span>
                        <div className="item-text-wrapper">
                          <h4>{q.statement || q.catalogName}</h4>
                          <span className="item-type-label">{typeConfig.title}</span>
                        </div>
                        <button 
                          type="button" 
                          className="btn-add-item-action" 
                          onClick={() => onAddQuestion && onAddQuestion(q)}
                          aria-label="Añadir pregunta"
                        >
                          <MdAdd />
                        </button>
                      </article>
                    );
                  })}
                </div>
              )}

              {questions.length < totals.q && (
                <button
                  type="button"
                  className="btn-show-more-full"
                  disabled={isLoadingQuestions}
                  onClick={() => loadQuestions(pages.q + 1)}
                >
                  <span>{isLoadingQuestions ? 'Cargando...' : 'Ver más preguntas'}</span>
                  <MdKeyboardArrowDown />
                </button>
              )}
            </section>
          )}

          {showBlocks && (
            <section className="sidebar-section">
              <h3 className="sidebar-section-title">BLOQUES DE PREGUNTAS:</h3>

              <div className="sidebar-search-wrapper single-field">
                <MdSearch className="search-icon-inside" />
                <input
                  type="text"
                  placeholder="Buscar bloques..."
                  value={blockSearch}
                  onChange={(e) => setBlockSearch(e.target.value)}
                />
              </div>

              {isLoadingBlocks && blocks.length === 0 ? (
                <Skeleton variant="list" count={2} hasActions={false} />
              ) : (
                <div className="sidebar-cards-list">
                  {filteredBlocks.map((b) => (
                    <article key={b.id} className="sidebar-item-card">
                      <span className="item-icon-box teal-block-icon">
                        <MdAdd />
                      </span>
                      <div className="item-text-wrapper">
                        <h4>{b.name}</h4>
                        <span className="item-type-label">{b.questionCount || 0} preguntas</span>
                      </div>
                      <div className="item-actions-double">
                        <button 
                          type="button" 
                          className="btn-add-item-action"
                          onClick={() => onAddBlock && onAddBlock(b)}
                          aria-label="Añadir bloque"
                        >
                          <MdAdd />
                        </button>
                        <button 
                          type="button" 
                          className="btn-preview-item-action"
                          onClick={() => onPreviewBlock && onPreviewBlock(b)}
                          aria-label="Ver bloque"
                        >
                          <MdOutlineVisibility />
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>
      </aside>
    </>
  );
}