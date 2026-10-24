import { useEffect, useMemo, useRef, useState } from 'react';
import {
  MdClose,
  MdKeyboardArrowRight,
  MdOutlineDescription,
  MdOutlineInsertDriveFile,
  MdUploadFile,
} from 'react-icons/md';

import { getCatalogTemplates } from '../../api/catalogApi.js';
import { useModalFocus } from '../../hooks/useModalFocus.js';
import { SearchToolbar } from '../SearchToolbar/SearchToolbar.jsx';
import { Skeleton } from '../Skeleton/Skeleton.jsx';

import './SurveyOriginModal.css';

const TEMPLATE_SEARCH_OPTIONS = [
  { value: 'title', label: 'Título' },
  { value: 'description', label: 'Descripción' },
];

export function SurveyOriginModal({
  isOpen,
  selectedOrigin,
  onClose,
  onImport,
  onSelect,
  returnFocusRef,
}) {
  const [view, setView] = useState('CHOICE');
  const [templates, setTemplates] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedSearchFields, setSelectedSearchFields] = useState(
    () => TEMPLATE_SEARCH_OPTIONS.map((option) => option.value),
  );
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const dialogRef = useRef(null);
  const titleRef = useRef(null);
  const searchInputRef = useRef(null);

  useModalFocus({
    isOpen,
    onClose: handleClose,
    dialogRef,
    initialFocusRef: titleRef,
    returnFocusRef,
  });

  useEffect(() => {
    if (!isOpen || view !== 'TEMPLATES') return undefined;
    const focusFrame = window.requestAnimationFrame(() => {
      searchInputRef.current?.focus({ preventScroll: true });
    });
    return () => window.cancelAnimationFrame(focusFrame);
  }, [isOpen, view]);

  function resetModal() {
    setView('CHOICE');
    setSearch('');
    setSelectedSearchFields(TEMPLATE_SEARCH_OPTIONS.map((option) => option.value));
    setLoadError('');
  }

  function handleClose() {
    resetModal();
    onClose();
  }

  function handleSelect(origin) {
    resetModal();
    onSelect(origin);
  }

  function handleImport() {
    resetModal();
    onImport();
  }

  async function showTemplates() {
    setView('TEMPLATES');
    if (templates.length > 0 || isLoading) return;

    try {
      setIsLoading(true);
      setLoadError('');
      const response = await getCatalogTemplates({ page: 0, size: 100 });
      setTemplates(Array.isArray(response) ? response : response?.content || []);
    } catch {
      setLoadError('No se pudieron cargar las plantillas del catálogo.');
    } finally {
      setIsLoading(false);
    }
  }

  const visibleTemplates = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('es');
    return templates.filter((template) => selectedSearchFields.some((field) => (
      template[field]?.toLocaleLowerCase('es').includes(query)
    )));
  }, [search, selectedSearchFields, templates]);

  if (!isOpen) return null;

  return (
    <div
      className="survey-origin-overlay"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) handleClose();
      }}
    >
      <section
        ref={dialogRef}
        tabIndex={-1}
        className={`survey-origin-modal survey-origin-modal-${view.toLocaleLowerCase()}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="survey-origin-title"
      >
        <header>
          <div>
            <h2 ref={titleRef} id="survey-origin-title" tabIndex={-1}>
              {view === 'CHOICE' ? '¿CÓMO QUIERES EMPEZAR?' : 'SELECCIONA UNA PLANTILLA'}
            </h2>
            <p>
              {view === 'CHOICE'
                ? 'Elige el punto de partida de la nueva encuesta.'
                : 'La plantilla se copiará y la encuesta será independiente.'}
            </p>
          </div>
          <button
            type="button"
            className="survey-origin-close"
            aria-label={selectedOrigin ? 'Cerrar selector de origen' : 'Cancelar creación de encuesta'}
            onClick={handleClose}
          >
            <MdClose aria-hidden="true" />
          </button>
        </header>

        {view === 'CHOICE' ? (
          <div className="survey-origin-options">
            <button type="button" onClick={() => handleSelect({ type: 'BLANK' })}>
              <span className="survey-origin-option-icon">
                <MdOutlineInsertDriveFile aria-hidden="true" />
              </span>
              <span>
                <strong>Empezar desde cero</strong>
                <small>Crea un borrador sin contenido y añade después su estructura.</small>
              </span>
              <span className="survey-origin-option-action">
                Seleccionar <MdKeyboardArrowRight aria-hidden="true" />
              </span>
            </button>
            <button type="button" onClick={showTemplates}>
              <span className="survey-origin-option-icon">
                <MdOutlineDescription aria-hidden="true" />
              </span>
              <span>
                <strong>Usar una plantilla del catálogo</strong>
                <small>Copia una estructura del catálogo como punto de partida.</small>
              </span>
              <span className="survey-origin-option-action">
                Ver plantillas <MdKeyboardArrowRight aria-hidden="true" />
              </span>
            </button>
            <button type="button" onClick={handleImport}>
              <span className="survey-origin-option-icon"><MdUploadFile aria-hidden="true" /></span>
              <span>
                <strong>Importar un archivo ODT</strong>
                <small>Genera un borrador editable desde la plantilla de FormQuid.</small>
              </span>
              <span className="survey-origin-option-action">
                Importar documento <MdKeyboardArrowRight aria-hidden="true" />
              </span>
            </button>
          </div>
        ) : (
          <>
            <SearchToolbar
              ariaLabel="Búsqueda y filtros de plantillas"
              searchLabel="Buscar plantillas"
              searchPlaceholder="Buscar"
              searchValue={search}
              searchInputRef={searchInputRef}
              onSearchChange={(event) => setSearch(event.target.value)}
              filterOptions={TEMPLATE_SEARCH_OPTIONS}
              filterOptionsControl="multiselect"
              filterOptionsAllLabel="Todos los campos"
              filterOptionsNoneLabel="Ningún campo"
              filterOptionsSelectedCountLabel="campos seleccionados"
              filterOptionsLabel="Campos de búsqueda"
              selectedFilterValues={selectedSearchFields}
              onFilterChange={setSelectedSearchFields}
              onFilterReset={() => {
                setSearch('');
                setSelectedSearchFields(TEMPLATE_SEARCH_OPTIONS.map((option) => option.value));
              }}
            />

            <div className="survey-template-list">
              {isLoading ? (
                <Skeleton variant="compact" count={3} hasActions={false} label="Cargando plantillas…" />
              ) : loadError ? (
                <p className="survey-template-status error" role="alert">{loadError}</p>
              ) : visibleTemplates.length === 0 ? (
                <p className="survey-template-status">No hay plantillas disponibles.</p>
              ) : visibleTemplates.map((template) => (
                <button
                  key={template.id}
                  type="button"
                  className="survey-template-option"
                  onClick={() => handleSelect({ type: 'TEMPLATE', template })}
                >
                  <span className="survey-template-image">
                    {template.imageKey
                      ? <img src={template.imageKey} alt="" />
                      : <MdOutlineDescription aria-hidden="true" />}
                  </span>
                  <span>
                    <strong>{template.title}</strong>
                    <small>{template.description || 'Sin descripción'}</small>
                  </span>
                  <MdKeyboardArrowRight aria-hidden="true" />
                </button>
              ))}
            </div>

            <footer>
              <button type="button" className="survey-origin-back" onClick={() => setView('CHOICE')}>
                Volver
              </button>
            </footer>
          </>
        )}
      </section>
    </div>
  );
}
