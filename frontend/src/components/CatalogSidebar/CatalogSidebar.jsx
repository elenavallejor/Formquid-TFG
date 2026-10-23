import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { MdAdd, MdChevronLeft, MdChevronRight, MdKeyboardArrowDown, MdLayers, MdOutlineVisibility, MdSearch } from 'react-icons/md';

import { createPortal } from 'react-dom';

import { getCatalogBlocksForBuilder, getCatalogQuestionsForBuilder } from '../../api/catalogApi.js';
import { CATEGORY_ICON_COMPONENTS, getCategoryIconName } from '../../utils/categoryIconOptions.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { CatalogBlockDetailsModal } from '../CatalogBlockDetailsModal/CatalogBlockDetailsModal.jsx';
import { Dropdown } from '../Dropdown/Dropdown.jsx';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './CatalogSidebar.css';

/**
 * Muestra los elementos habilitados del catálogo y permite añadirlos o arrastrarlos hacia un constructor.
 */
export function CatalogSidebar(props) {
  const categoryKey = props.categoryFilter == null ? 'all' : `category:${props.categoryFilter}`;
  return <CatalogSidebarContent key={categoryKey} {...props} />;
}

function EmptyCategoryMessage({ itemType, category }) {
  const Icon = CATEGORY_ICON_COMPONENTS[getCategoryIconName(category)];
  return (
    <p className="catalog-empty" role="status">
      No hay {itemType} disponibles para la categoría{' '}
      <span className="catalog-empty-category"><Icon aria-hidden="true" /><span>{category.name}</span></span>.
    </p>
  );
}

function CatalogSidebarContent({
  showQuestions = true,
  showBlocks = true,
  allowedQuestionTypes,
  categoryFilter = null,
  categoryDetails = null,
  onAddQuestion,
  onAddBlock,
  defaultOpen,
  headerSelector = '#main-header',
  embedded = false,
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
  const [isOpen, setIsOpen] = useState(() => embedded || (defaultOpen ?? !(typeof window !== 'undefined' && window.matchMedia('(max-width: 700px)').matches)));
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
  const [selectedBlock, setSelectedBlock] = useState(null);

  const availableQuestionTypes = useMemo(() => {
    if (!allowedQuestionTypes) return QUESTION_TYPES;
    const allowedTypes = new Set(allowedQuestionTypes);
    return QUESTION_TYPES.filter((type) => allowedTypes.has(type.id));
  }, [allowedQuestionTypes]);
  const activeTypeFilter = availableQuestionTypes.some((type) => type.id === typeFilter) ? typeFilter : '';
  const requiresCategory = categoryFilter !== null;
  const canLoadCatalog = !requiresCategory || Boolean(categoryFilter);
  const selectedCategory = categoryDetails?.code === categoryFilter
    ? categoryDetails
    : { code: categoryFilter, name: categoryFilter?.replaceAll('_', ' ').toLocaleLowerCase('es').replace(/^./, (letter) => letter.toLocaleUpperCase('es')) };
  const isInitialQuestionLoad = showQuestions && !hasLoadedQuestions && !questionError;
  const isInitialBlockLoad = showBlocks && !hasLoadedBlocks && !blockError;

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
    if (!canLoadCatalog) return;
    if (questionRequestRef.current && !questionRequestRef.current.signal.aborted) return;
    const controller = createRequestController();
    questionRequestRef.current = controller;
    try {
      const response = await getCatalogQuestionsForBuilder({ page, size: 10, category: categoryFilter || undefined, signal: controller.signal });
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
  }, [canLoadCatalog, categoryFilter, createRequestController]);

  const loadBlocks = useCallback(async (page = 0) => {
    if (!canLoadCatalog) return;
    if (blockRequestRef.current && !blockRequestRef.current.signal.aborted) return;
    const controller = createRequestController();
    blockRequestRef.current = controller;
    try {
      const response = await getCatalogBlocksForBuilder({ page, size: 10, category: categoryFilter || undefined, signal: controller.signal });
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
  }, [canLoadCatalog, categoryFilter, createRequestController]);

  useEffect(() => {
    if (!canLoadCatalog || !showQuestions || hasLoadedQuestions || questionError || questionRequestRef.current) return undefined;
    const controller = createRequestController();
    const controllers = requestControllersRef.current;
    questionRequestRef.current = controller;

    getCatalogQuestionsForBuilder({ page: 0, size: 10, category: categoryFilter || undefined, signal: controller.signal })
      .then((response) => {
        if (controller.signal.aborted) return;
        const items = Array.isArray(response) ? response : response?.content ?? [];
        setQuestions(items);
        setQuestionTotal(response?.totalElements ?? items.length);
        setQuestionPage(0);
        setHasLoadedQuestions(true);
      })
      .catch((error) => {
        if (!controller.signal.aborted && error?.name !== 'AbortError') {
          setQuestionError(error?.message || 'No se han podido cargar las preguntas del catálogo.');
        }
      })
      .finally(() => {
        controllers.delete(controller);
        if (questionRequestRef.current === controller) questionRequestRef.current = null;
      });

    return () => {
      controller.abort();
      controllers.delete(controller);
      if (questionRequestRef.current === controller) questionRequestRef.current = null;
    };
  }, [canLoadCatalog, categoryFilter, showQuestions, hasLoadedQuestions, questionError, createRequestController]);

  useEffect(() => {
    if (!canLoadCatalog || !showBlocks || hasLoadedBlocks || blockError || blockRequestRef.current) return undefined;
    const controller = createRequestController();
    const controllers = requestControllersRef.current;
    blockRequestRef.current = controller;

    getCatalogBlocksForBuilder({ page: 0, size: 10, category: categoryFilter || undefined, signal: controller.signal })
      .then((response) => {
        if (controller.signal.aborted) return;
        const items = Array.isArray(response) ? response : response?.content ?? [];
        setBlocks(items);
        setBlockTotal(response?.totalElements ?? items.length);
        setBlockPage(0);
        setHasLoadedBlocks(true);
      })
      .catch((error) => {
        if (!controller.signal.aborted && error?.name !== 'AbortError') {
          setBlockError(error?.message || 'No se han podido cargar los bloques del catálogo.');
        }
      })
      .finally(() => {
        controllers.delete(controller);
        if (blockRequestRef.current === controller) blockRequestRef.current = null;
      });

    return () => {
      controller.abort();
      controllers.delete(controller);
      if (blockRequestRef.current === controller) blockRequestRef.current = null;
    };
  }, [canLoadCatalog, categoryFilter, showBlocks, hasLoadedBlocks, blockError, createRequestController]);

  function requestQuestions(page = 0) {
    setIsLoadingQuestions(true);
    setQuestionError('');
    void loadQuestions(page);
  }

  function requestBlocks(page = 0) {
    setIsLoadingBlocks(true);
    setBlockError('');
    void loadBlocks(page);
  }

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
    if (embedded || !isMobile || !isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeRef.current?.focus({ preventScroll: true });
    return () => { document.body.style.overflow = previousOverflow; };
  }, [embedded, isMobile, isOpen]);

  useEffect(() => {
    if (embedded || !shouldMoveFocusRef.current) return;
    const animationFrame = window.requestAnimationFrame(() => {
      (isOpen ? closeRef : toggleRef).current?.focus({ preventScroll: true });
      shouldMoveFocusRef.current = false;
    });
    return () => window.cancelAnimationFrame(animationFrame);
  }, [embedded, isOpen]);

  useEffect(() => {
    if (embedded || !isOpen || selectedBlock) return undefined;

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
  }, [closeSidebar, embedded, isMobile, isOpen, selectedBlock]);

  const filteredQuestions = useMemo(() => {
    const search = questionSearch.trim().toLocaleLowerCase('es');
    const visibleTypes = new Set(availableQuestionTypes.map((type) => type.id));
    return questions.filter((question) => {
      const text = question.statement || '';
      return (!requiresCategory || question.category === categoryFilter)
        && visibleTypes.has(question.questionType)
        && (!search || text.toLocaleLowerCase('es').includes(search))
        && (!activeTypeFilter || question.questionType === activeTypeFilter);
    });
  }, [activeTypeFilter, availableQuestionTypes, categoryFilter, questionSearch, questions, requiresCategory]);

  const filteredBlocks = useMemo(() => {
    const search = blockSearch.trim().toLocaleLowerCase('es');
    return blocks.filter((block) => (!requiresCategory || block.category === categoryFilter)
      && (!search || (block.name || block.title || '').toLocaleLowerCase('es').includes(search)));
  }, [blockSearch, blocks, categoryFilter, requiresCategory]);

  function handleDragStart(event, type, item) {
    if (requiresCategory && item.category !== categoryFilter) {
      event.preventDefault();
      return;
    }
    const payload = JSON.stringify(type === 'QUESTION' ? { type: 'CATALOG_QUESTION', question: item } : { type: 'CATALOG_BLOCK', block: item });
    event.dataTransfer.setData(
      type === 'QUESTION' ? 'application/x-formquid-question' : 'application/x-formquid-block',
      '1',
    );
    event.dataTransfer.setData('application/x-formquid-catalog-item', payload);
    event.dataTransfer.setData('text/plain', payload);
    event.dataTransfer.effectAllowed = 'copy';
    event.currentTarget.classList.add('dragging');
  }

  async function handleAdd(type, item) {
    const callback = type === 'QUESTION' ? onAddQuestion : onAddBlock;
    if (!callback || pendingActionRef.current) return;
    if (requiresCategory && (!categoryFilter || item.category !== categoryFilter)) return;
    const existingItems = new Set(document.querySelectorAll('.block-questions-list > .block-question-item'));
    pendingActionRef.current = true;
    const actionId = `${type}-${item.id}`;
    setPendingAction(actionId);
    setActionError('');

    try {
      const added = await callback(item);
      if (added === false) return;
      if (isMobile && !embedded) closeSidebar();
      window.requestAnimationFrame(() => {
        const addedItem = [...document.querySelectorAll('.block-questions-list > .block-question-item')]
          .find((element) => !existingItems.has(element));
        const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        addedItem?.scrollIntoView?.({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'center', inline: 'nearest' });
      });
    } catch (error) {
      setActionError(error?.message || 'No se ha podido añadir el elemento.');
    } finally {
      pendingActionRef.current = false;
      setPendingAction('');
    }
  }

  function renderQuestionList() {
    if (!canLoadCatalog) return <p className="catalog-empty" role="status">Selecciona una categoría para ver las preguntas disponibles.</p>;
    if ((isLoadingQuestions || isInitialQuestionLoad) && questions.length === 0) return <Skeleton variant="list" count={4} hasActions={false} />;
    if (questionError && questions.length === 0) {
      return (
        <div className="catalog-error" role="alert">
          <p>{questionError}</p>
          <button type="button" onClick={() => requestQuestions()}>Reintentar</button>
        </div>
      );
    }
    if (categoryFilter && !questions.some((question) => question.category === categoryFilter)) {
      return <EmptyCategoryMessage itemType="preguntas" category={selectedCategory} />;
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
    if (!canLoadCatalog) return <p className="catalog-empty" role="status">Selecciona una categoría para ver los bloques disponibles.</p>;
    if ((isLoadingBlocks || isInitialBlockLoad) && blocks.length === 0) return <Skeleton variant="list" count={2} hasActions={false} />;
    if (blockError && blocks.length === 0) {
      return (
        <div className="catalog-error" role="alert">
          <p>{blockError}</p>
          <button type="button" onClick={() => requestBlocks()}>Reintentar</button>
        </div>
      );
    }
    if (categoryFilter && !blocks.some((block) => block.category === categoryFilter)) {
      return <EmptyCategoryMessage itemType="bloques" category={selectedCategory} />;
    }
    if (filteredBlocks.length === 0) return <p className="catalog-empty" role="status">No se han encontrado bloques que coincidan con la búsqueda.</p>;

    return (
      <div className="catalog-list">
        {filteredBlocks.map((block) => {
          const blockName = block.name || block.title || 'Bloque sin título';
          const actionId = `BLOCK-${block.id}`;
          const questionCount = block.questionCount
            ?? block.contentItems?.filter((item) => item.itemType === 'QUESTION').length
            ?? block.questions?.length
            ?? block.items?.filter((item) => item.itemType === 'QUESTION').length
            ?? 0;

          return (
            <article key={block.id} className="catalog-item" draggable aria-describedby={instructionsId} onDragStart={(event) => handleDragStart(event, 'BLOCK', block)} onDragEnd={(event) => event.currentTarget.classList.remove('dragging')}>
              <span className="catalog-icon catalog-block-icon" aria-hidden="true"><MdLayers /></span>
              <div className="catalog-info"><h4>{blockName.toUpperCase()}</h4><span>{questionCount} {questionCount === 1 ? 'pregunta' : 'preguntas'}</span></div>
              <div className="catalog-actions">
                <button
                  type="button"
                  className="catalog-action"
                  aria-label={`Ver detalles del bloque: ${blockName}`}
                  onClick={() => setSelectedBlock(block)}
                >
                  <MdOutlineVisibility aria-hidden="true" />
                </button>
                {onAddBlock && <button type="button" className="catalog-action" aria-label={`Añadir bloque: ${blockName}`} aria-busy={pendingAction === actionId} disabled={Boolean(pendingAction)} onClick={() => handleAdd('BLOCK', block)}><MdAdd aria-hidden="true" /></button>}
              </div>
            </article>
          );
        })}
      </div>
    );
  }

  const sidebar = (
      <aside ref={sidebarRef} id={sidebarId} className={`catalog-sidebar ${isOpen ? 'open' : ''} ${embedded ? 'catalog-sidebar-embedded' : ''}`} aria-label="Catálogo de elementos" aria-hidden={selectedBlock ? true : embedded ? undefined : !isOpen} inert={selectedBlock || (!embedded && !isOpen) ? true : undefined} aria-modal={!selectedBlock && !embedded && isMobile && isOpen ? 'true' : undefined} role={!embedded && isMobile ? 'dialog' : undefined}>
        <header className="catalog-header">
          <h2>{embedded ? 'CATÁLOGO DE PREGUNTAS' : 'CATÁLOGO DE ELEMENTOS'}</h2>
          {!embedded && <button ref={closeRef} type="button" className="catalog-close" aria-label="Cerrar catálogo de elementos" aria-controls={sidebarId} onClick={closeSidebar}><MdChevronLeft aria-hidden="true" /></button>}
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
                  <Dropdown aria-label="Filtrar preguntas por tipo" className={activeTypeFilter ? '' : 'catalog-filter-placeholder'} value={activeTypeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
                    <option value="">Filtrar</option>
                    {availableQuestionTypes.map((type) => <option key={type.id} value={type.id}>{type.title}</option>)}
                  </Dropdown>
                </div>
              </div>
              {renderQuestionList()}
              {questionError && questions.length > 0 && <p className="catalog-action-error" role="alert">{questionError}</p>}
              {questions.length < questionTotal && <button type="button" className="catalog-more" disabled={isLoadingQuestions} aria-busy={isLoadingQuestions} onClick={() => requestQuestions(questionPage + 1)}><span>{isLoadingQuestions ? 'Cargando...' : 'Ver más preguntas'}</span><MdKeyboardArrowDown aria-hidden="true" /></button>}
            </section>
          )}

          {showBlocks && (
            <section className="catalog-section" aria-labelledby={blocksTitleId}>
              <h3 id={blocksTitleId} className="catalog-title">BLOQUES DE PREGUNTAS:</h3>
              <div className="catalog-search catalog-search-full"><MdSearch aria-hidden="true" /><input type="search" aria-label="Buscar bloques de preguntas" placeholder="Buscar bloques..." value={blockSearch} onChange={(event) => setBlockSearch(event.target.value)} /></div>
              {renderBlockList()}
              {blockError && blocks.length > 0 && <p className="catalog-action-error" role="alert">{blockError}</p>}
              {blocks.length < blockTotal && <button type="button" className="catalog-more" disabled={isLoadingBlocks} aria-busy={isLoadingBlocks} onClick={() => requestBlocks(blockPage + 1)}><span>{isLoadingBlocks ? 'Cargando...' : 'Ver más bloques'}</span><MdKeyboardArrowDown aria-hidden="true" /></button>}
            </section>
          )}
          </div>
      </aside>
  );

  const detailsModal = selectedBlock && (
    <CatalogBlockDetailsModal block={selectedBlock} onClose={() => setSelectedBlock(null)} />
  );

  if (embedded) return <><div className="catalog-embedded-root">{sidebar}</div>{detailsModal}</>;

  return (
    <>
      <div className={`catalog-space ${isOpen ? 'open' : ''}`} aria-hidden="true" />
      {typeof document !== 'undefined' && createPortal(
        <div ref={overlayRef} className="catalog-overlay-root">
      <button ref={toggleRef} type="button" className={`catalog-toggle ${isOpen ? 'catalog-toggle-hidden' : ''}`} aria-label="Abrir catálogo de elementos" title="Abrir catálogo de elementos" aria-controls={sidebarId} aria-expanded={isOpen} onClick={openSidebar}>
        <MdLayers aria-hidden="true" /><span>CATÁLOGO</span><MdChevronRight aria-hidden="true" />
      </button>

      {sidebar}

      <button type="button" className={`catalog-backdrop ${isOpen ? 'visible' : ''}`} aria-hidden="true" tabIndex={-1} onClick={closeSidebar} />
        </div>,
        document.body,
      )}
      {detailsModal}
    </>
  );
}
