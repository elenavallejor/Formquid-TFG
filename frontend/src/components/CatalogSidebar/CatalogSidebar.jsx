import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { MdAdd, MdChevronLeft, MdChevronRight, MdKeyboardArrowDown, MdLayers, MdOutlineVisibility, MdSearch } from 'react-icons/md';

import { createPortal } from 'react-dom';

import { getCatalogBlocks, getCatalogQuestionsForBuilder } from '../../api/catalogApi.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './CatalogSidebar.css';

/**
 * Muestra los elementos habilitados del catálogo y permite añadirlos o arrastrarlos hacia un constructor.
 */
export function CatalogSidebar({
  showQuestions = true,
  showBlocks = true,
  allowedQuestionTypes,
  onAddQuestion,
  onAddBlock,
  onPreviewBlock,
  defaultOpen,
  headerSelector = '#main-header',
}) {
  const sidebarId = `catalog-sidebar-${useId().replaceAll(':', '')}`;
  const questionsTitleId = `${sidebarId}-questions-title`;
  const blocksTitleId = `${sidebarId}-blocks-title`;
  const instructionsId = `${sidebarId}-instructions`;
  const sidebarRef = useRef(null);
  const overlayRef = useRef(null);
  const pendingActionRef = useRef(false);
  const questionRequestRef = useRef(null);
  const blockRequestRef = useRef(null);
  const toggleRef = useRef(null);
  const closeRef = useRef(null);
  const shouldMoveFocusRef = useRef(false);
  const requestControllersRef = useRef(new Set());
  const [isMobile, setIsMobile] = useState(() => typeof window !== 'undefined' && window.matchMedia('(max-width: 700px)').matches);
  const [isOpen, setIsOpen] = useState(() => defaultOpen ?? !(typeof window !== 'undefined' && window.matchMedia('(max-width: 700px)').matches));
  const [questions, setQuestions] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [questionSearch, setQuestionSearch] = useState('');
  const [blockSearch, setBlockSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [questionError, setQuestionError] = useState('');
  const [blockError, setBlockError] = useState('');
  const [isLoadingQuestions, setIsLoadingQuestions] = useState(false);
  const [isLoadingBlocks, setIsLoadingBlocks] = useState(false);
  const [hasLoadedQuestions, setHasLoadedQuestions] = useState(false);
  const [hasLoadedBlocks, setHasLoadedBlocks] = useState(false);
  const [questionPage, setQuestionPage] = useState(0);
  const [blockPage, setBlockPage] = useState(0);
  const [questionTotal, setQuestionTotal] = useState(0);
  const [blockTotal, setBlockTotal] = useState(0);
  const [pendingAction, setPendingAction] = useState('');
  const [actionError, setActionError] = useState('');

  const availableQuestionTypes = useMemo(() => {
    if (!allowedQuestionTypes) return QUESTION_TYPES;
    const allowedTypes = new Set(allowedQuestionTypes);
    return QUESTION_TYPES.filter((type) => allowedTypes.has(type.id));
  }, [allowedQuestionTypes]);

  const createRequestController = useCallback(() => {
    const controller = new AbortController();
    requestControllersRef.current.add(controller);
    return controller;
  }, []);

  const openSidebar = useCallback(() => {
    shouldMoveFocusRef.current = true;
    setIsOpen(true);
  }, []);

  const closeSidebar = useCallback(() => {
    shouldMoveFocusRef.current = true;
    setIsOpen(false);
  }, []);

  const loadQuestions = useCallback(async (page = 0) => {
    if (questionRequestRef.current && !questionRequestRef.current.signal.aborted) return;
    const controller = createRequestController();
    questionRequestRef.current = controller;
    setIsLoadingQuestions(true);
    setQuestionError('');

    try {
      const response = await getCatalogQuestionsForBuilder({ page, size: 10, signal: controller.signal });
      if (controller.signal.aborted) return;
      const items = Array.isArray(response) ? response : response?.content ?? [];
      setQuestions((current) => page === 0 ? items : [...current, ...items]);
      setQuestionTotal(response?.totalElements ?? items.length);
      setQuestionPage(page);
      setHasLoadedQuestions(true);
    } catch (error) {
      if (!controller.signal.aborted && error?.name !== 'AbortError') setQuestionError(error?.message || 'No se han podido cargar las preguntas del catálogo.');
    } finally {
      requestControllersRef.current.delete(controller);
      if (questionRequestRef.current === controller) {
        questionRequestRef.current = null;
        setIsLoadingQuestions(false);
      }
    }
  }, [createRequestController]);

  const loadBlocks = useCallback(async (page = 0) => {
    if (blockRequestRef.current && !blockRequestRef.current.signal.aborted) return;
    const controller = createRequestController();
    blockRequestRef.current = controller;
    setIsLoadingBlocks(true);
    setBlockError('');

    try {
      const response = await getCatalogBlocks({ page, size: 10, signal: controller.signal });
      if (controller.signal.aborted) return;
      const items = Array.isArray(response) ? response : response?.content ?? [];
      setBlocks((current) => page === 0 ? items : [...current, ...items]);
      setBlockTotal(response?.totalElements ?? items.length);
      setBlockPage(page);
      setHasLoadedBlocks(true);
    } catch (error) {
      if (!controller.signal.aborted && error?.name !== 'AbortError') setBlockError(error?.message || 'No se han podido cargar los bloques del catálogo.');
    } finally {
      requestControllersRef.current.delete(controller);
      if (blockRequestRef.current === controller) {
        blockRequestRef.current = null;
        setIsLoadingBlocks(false);
      }
    }
  }, [createRequestController]);

  useEffect(() => {
    if (isOpen && showQuestions && !hasLoadedQuestions && !isLoadingQuestions && !questionError) loadQuestions();
  }, [isOpen, showQuestions, hasLoadedQuestions, isLoadingQuestions, questionError, loadQuestions]);

  useEffect(() => {
    if (isOpen && showBlocks && !hasLoadedBlocks && !isLoadingBlocks && !blockError) loadBlocks();
  }, [isOpen, showBlocks, hasLoadedBlocks, isLoadingBlocks, blockError, loadBlocks]);

  useEffect(() => {
    if (typeFilter && !availableQuestionTypes.some((type) => type.id === typeFilter)) setTypeFilter('');
  }, [availableQuestionTypes, typeFilter]);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 700px)');
    const handleChange = (event) => setIsMobile(event.matches);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  useEffect(() => () => {
    requestControllersRef.current.forEach((controller) => controller.abort());
    requestControllersRef.current.clear();
  }, []);

  useEffect(() => {
    const host = overlayRef.current;
    if (!host) return;
    const header = document.querySelector(headerSelector);
    let frame = 0;
    function updatePosition() {
      frame = 0;
      const top = Math.max(0, Math.min(header?.getBoundingClientRect().bottom ?? 0, window.innerHeight));
      host.style.setProperty('--catalog-top', `${top}px`);
    }
    function scheduleUpdate() {
      if (!frame) frame = window.requestAnimationFrame(updatePosition);
    }
    const observer = new ResizeObserver(scheduleUpdate);
    if (header) observer.observe(header);
    window.addEventListener('scroll', scheduleUpdate, { passive: true, capture: true });
    window.addEventListener('resize', scheduleUpdate);
    window.visualViewport?.addEventListener('resize', scheduleUpdate);
    updatePosition();
    return () => {
      observer.disconnect();
      window.cancelAnimationFrame(frame);
      window.removeEventListener('scroll', scheduleUpdate, true);
      window.removeEventListener('resize', scheduleUpdate);
      window.visualViewport?.removeEventListener('resize', scheduleUpdate);
    };
  }, [headerSelector]);

  useEffect(() => {
    if (!isMobile || !isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeRef.current?.focus({ preventScroll: true });
    return () => { document.body.style.overflow = previousOverflow; };
  }, [isMobile, isOpen]);

  useEffect(() => {
    if (!shouldMoveFocusRef.current) return;
    const animationFrame = window.requestAnimationFrame(() => {
      (isOpen ? closeRef : toggleRef).current?.focus({ preventScroll: true });
      shouldMoveFocusRef.current = false;
    });
    return () => window.cancelAnimationFrame(animationFrame);
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return undefined;

    function handleKeyDown(event) {
      if (event.defaultPrevented) return;
      if (event.key === 'Escape' && sidebarRef.current?.contains(event.target)) {
        event.preventDefault();
        closeSidebar();
        return;
      }

      if (!isMobile || event.key !== 'Tab') return;
      const focusableElements = [...sidebarRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), [href], [tabindex]:not([tabindex="-1"])')];
      if (focusableElements.length === 0) return;
      const firstElement = focusableElements[0];
      const lastElement = focusableElements.at(-1);

      if (event.shiftKey && (document.activeElement === firstElement || !sidebarRef.current.contains(document.activeElement))) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && (document.activeElement === lastElement || !sidebarRef.current.contains(document.activeElement))) {
        event.preventDefault();
        firstElement.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [closeSidebar, isMobile, isOpen]);

  const filteredQuestions = useMemo(() => {
    const search = questionSearch.trim().toLocaleLowerCase('es');
    const visibleTypes = new Set(availableQuestionTypes.map((type) => type.id));
    return questions.filter((question) => {
      const text = question.statement || '';
      return visibleTypes.has(question.questionType) && (!search || text.toLocaleLowerCase('es').includes(search)) && (!typeFilter || question.questionType === typeFilter);
    });
  }, [availableQuestionTypes, questionSearch, questions, typeFilter]);

  const filteredBlocks = useMemo(() => {
    const search = blockSearch.trim().toLocaleLowerCase('es');
    return blocks.filter((block) => !search || (block.name || block.title || '').toLocaleLowerCase('es').includes(search));
  }, [blockSearch, blocks]);

  function handleDragStart(event, type, item) {
    const payload = JSON.stringify(type === 'QUESTION' ? { type: 'CATALOG_QUESTION', question: item } : { type: 'CATALOG_BLOCK', block: item });
    event.dataTransfer.setData('application/x-formquid-catalog-item', payload);
    event.dataTransfer.setData('text/plain', payload);
    event.dataTransfer.effectAllowed = 'copy';
    event.currentTarget.classList.add('dragging');
  }

  async function handleAdd(type, item) {
    const callback = type === 'QUESTION' ? onAddQuestion : onAddBlock;
    if (!callback || pendingActionRef.current) return;
    pendingActionRef.current = true;
    const actionId = `${type}-${item.id}`;
    setPendingAction(actionId);
    setActionError('');

    try {
      await callback(item);
      if (isMobile) closeSidebar();
    } catch (error) {
      setActionError(error?.message || 'No se ha podido añadir el elemento.');
    } finally {
      pendingActionRef.current = false;
      setPendingAction('');
    }
  }

  function renderQuestionList() {
    if (isLoadingQuestions && questions.length === 0) return <Skeleton variant="list" count={4} hasActions={false} />;
    if (questionError && questions.length === 0) {
      return (
        <div className="catalog-error" role="alert">
          <p>{questionError}</p>
          <button type="button" onClick={() => loadQuestions()}>Reintentar</button>
        </div>
      );
    }
    if (filteredQuestions.length === 0) return <p className="catalog-empty" role="status">No se han encontrado preguntas que coincidan con la búsqueda o el filtro seleccionado.</p>;

    return (
      <div className="catalog-list">
        {filteredQuestions.map((question) => {
          const typeConfig = QUESTION_TYPES.find((type) => type.id === question.questionType) ?? QUESTION_TYPES[0];
          const Icon = typeConfig.icon;
          const questionName = question.statement || 'Pregunta sin enunciado';
          const actionId = `QUESTION-${question.id}`;

          return (
            <article key={question.id} className="catalog-item" draggable aria-describedby={instructionsId} onDragStart={(event) => handleDragStart(event, 'QUESTION', question)} onDragEnd={(event) => event.currentTarget.classList.remove('dragging')}>
              <span className="catalog-icon" style={{ color: typeConfig.color, borderColor: typeConfig.color }} aria-hidden="true"><Icon /></span>
              <div className="catalog-info"><h4>{questionName}</h4><span>{typeConfig.title}</span></div>
              {onAddQuestion && (
                <button type="button" className="catalog-action" aria-label={`Añadir pregunta: ${questionName}`} aria-busy={pendingAction === actionId} disabled={Boolean(pendingAction)} onClick={() => handleAdd('QUESTION', question)}>
                  <MdAdd aria-hidden="true" />
                </button>
              )}
            </article>
          );
        })}
      </div>
    );
  }

  function renderBlockList() {
    if (isLoadingBlocks && blocks.length === 0) return <Skeleton variant="list" count={2} hasActions={false} />;
    if (blockError && blocks.length === 0) {
      return (
        <div className="catalog-error" role="alert">
          <p>{blockError}</p>
          <button type="button" onClick={() => loadBlocks()}>Reintentar</button>
        </div>
      );
    }
    if (filteredBlocks.length === 0) return <p className="catalog-empty" role="status">No se han encontrado bloques que coincidan con la búsqueda.</p>;

    return (
      <div className="catalog-list">
        {filteredBlocks.map((block) => {
          const blockName = block.name || block.title || 'Bloque sin título';
          const actionId = `BLOCK-${block.id}`;

          return (
            <article key={block.id} className="catalog-item" draggable aria-describedby={instructionsId} onDragStart={(event) => handleDragStart(event, 'BLOCK', block)} onDragEnd={(event) => event.currentTarget.classList.remove('dragging')}>
              <span className="catalog-icon catalog-block-icon" aria-hidden="true"><MdLayers /></span>
              <div className="catalog-info"><h4>{blockName}</h4><span>{block.questionCount ?? block.questions?.length ?? block.items?.filter((item) => item.itemType !== 'TEXT').length ?? 0} preguntas</span></div>
              {(onAddBlock || onPreviewBlock) && (
                <div className="catalog-actions">
                  {onAddBlock && <button type="button" className="catalog-action" aria-label={`Añadir bloque: ${blockName}`} aria-busy={pendingAction === actionId} disabled={Boolean(pendingAction)} onClick={() => handleAdd('BLOCK', block)}><MdAdd aria-hidden="true" /></button>}
                  {onPreviewBlock && <button type="button" className="catalog-action" aria-label={`Ver detalles del bloque: ${blockName}`} onClick={() => onPreviewBlock(block)}><MdOutlineVisibility aria-hidden="true" /></button>}
                </div>
              )}
            </article>
          );
        })}
      </div>
    );
  }

  return (
    <>
      <div className={`catalog-space ${isOpen ? 'open' : ''}`} aria-hidden="true" />
      {typeof document !== 'undefined' && createPortal(
        <div ref={overlayRef} className="catalog-overlay-root">
      <button ref={toggleRef} type="button" className={`catalog-toggle ${isOpen ? 'catalog-toggle-hidden' : ''}`} aria-label="Abrir catálogo de elementos" aria-controls={sidebarId} aria-expanded={isOpen} onClick={openSidebar}>
        <MdLayers aria-hidden="true" /><span>CATÁLOGO</span><MdChevronRight aria-hidden="true" />
      </button>

      <aside ref={sidebarRef} id={sidebarId} className={`catalog-sidebar ${isOpen ? 'open' : ''}`} aria-label="Catálogo de elementos" aria-hidden={!isOpen} inert={!isOpen} aria-modal={isMobile && isOpen ? 'true' : undefined} role={isMobile ? 'dialog' : undefined}>
        <header className="catalog-header">
          <h2>CATÁLOGO DE ELEMENTOS</h2>
          <button ref={closeRef} type="button" className="catalog-close" aria-label="Cerrar catálogo de elementos" aria-controls={sidebarId} onClick={closeSidebar}><MdChevronLeft aria-hidden="true" /></button>
        </header>

        <div className="catalog-content">
          <p id={instructionsId} className="sr-only">Utiliza el botón de añadir para colocar el elemento al final o arrástralo hasta la posición deseada en el constructor.</p>
          {actionError && <p className="catalog-action-error" role="alert">{actionError}</p>}

          {showQuestions && availableQuestionTypes.length > 0 && (
            <section className="catalog-section" aria-labelledby={questionsTitleId}>
              <h3 id={questionsTitleId} className="catalog-title">PREGUNTAS FIJAS:</h3>
              <div className="catalog-filters">
                <div className="catalog-search"><MdSearch aria-hidden="true" /><input type="search" aria-label="Buscar preguntas fijas" placeholder="Buscar preguntas..." value={questionSearch} onChange={(event) => setQuestionSearch(event.target.value)} /></div>
                <div className="catalog-select">
                  <select aria-label="Filtrar preguntas por tipo" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
                    <option value="">Filtrar por tipo</option>
                    {availableQuestionTypes.map((type) => <option key={type.id} value={type.id}>{type.title}</option>)}
                  </select>
                  <MdKeyboardArrowDown aria-hidden="true" />
                </div>
              </div>
              {renderQuestionList()}
              {questionError && questions.length > 0 && <p className="catalog-action-error" role="alert">{questionError}</p>}
              {questions.length < questionTotal && <button type="button" className="catalog-more" disabled={isLoadingQuestions} aria-busy={isLoadingQuestions} onClick={() => loadQuestions(questionPage + 1)}><span>{isLoadingQuestions ? 'Cargando...' : 'Ver más preguntas'}</span><MdKeyboardArrowDown aria-hidden="true" /></button>}
            </section>
          )}

          {showBlocks && (
            <section className="catalog-section" aria-labelledby={blocksTitleId}>
              <h3 id={blocksTitleId} className="catalog-title">BLOQUES DE PREGUNTAS:</h3>
              <div className="catalog-search catalog-search-full"><MdSearch aria-hidden="true" /><input type="search" aria-label="Buscar bloques de preguntas" placeholder="Buscar bloques..." value={blockSearch} onChange={(event) => setBlockSearch(event.target.value)} /></div>
              {renderBlockList()}
              {blockError && blocks.length > 0 && <p className="catalog-action-error" role="alert">{blockError}</p>}
              {blocks.length < blockTotal && <button type="button" className="catalog-more" disabled={isLoadingBlocks} aria-busy={isLoadingBlocks} onClick={() => loadBlocks(blockPage + 1)}><span>{isLoadingBlocks ? 'Cargando...' : 'Ver más bloques'}</span><MdKeyboardArrowDown aria-hidden="true" /></button>}
            </section>
          )}
        </div>
      </aside>

      <button type="button" className={`catalog-backdrop ${isOpen ? 'visible' : ''}`} aria-hidden="true" tabIndex={-1} onClick={closeSidebar} />
        </div>,
        document.body,
      )}
    </>
  );
}