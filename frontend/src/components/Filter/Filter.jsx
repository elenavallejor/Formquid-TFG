import { useEffect, useId, useRef, useState } from 'react';
import { IoFilter } from 'react-icons/io5';
import { MdClose } from 'react-icons/md';

import './Filter.css';

/** Filtro múltiple con menú flotante y aplicación explícita de la selección. */
export function Filter({ label = 'Filtrar', options, optionsLabel = 'Opciones', selectedValues, textFilter, onChange, onReset }) {
  const [open, setOpen] = useState(false);
  const [openUp, setOpenUp] = useState(false);
  const [draftValues, setDraftValues] = useState(selectedValues);
  const [draftText, setDraftText] = useState(textFilter?.value ?? '');
  const [isMobile, setIsMobile] = useState(
    () => typeof window !== 'undefined' && Boolean(window.matchMedia?.('(max-width: 767px)').matches),
  );
  const wrapperRef = useRef(null);
  const triggerRef = useRef(null);
  const modalRef = useRef(null);
  const mobileCloseRef = useRef(null);
  const firstOptionRef = useRef(null);
  const focusFirstOptionOnOpen = useRef(false);
  const menuId = useId();
  const modalTitleId = useId();
  const selectedLabel = selectedValues.length === options.length
    ? 'Todas'
    : selectedValues.length === 0
      ? 'Ninguna'
      : selectedValues.length === 1
        ? options.find((option) => option.value === selectedValues[0])?.label ?? 'Una seleccionada'
        : `${selectedValues.length} seleccionadas`;
  const appliedText = textFilter?.value?.trim() ?? '';
  const hasActiveFilters = selectedValues.length !== options.length || Boolean(appliedText);

  useEffect(() => {
    if (!window.matchMedia) return undefined;
    const mediaQuery = window.matchMedia('(max-width: 767px)');
    const handleChange = () => {
      setIsMobile(mediaQuery.matches);
      setOpen(false);
    };
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  useEffect(() => {
    if (!open || isMobile) return undefined;

    function handlePointerDown(event) {
      if (!wrapperRef.current?.contains(event.target)) setOpen(false);
    }

    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [open, isMobile]);

  useEffect(() => {
    if (!open || !isMobile || !modalRef.current) return undefined;

    const modal = modalRef.current;
    const previousOverflow = document.body.style.overflow;
    modal.showModal();
    mobileCloseRef.current?.focus();
    document.body.style.overflow = 'hidden';

    return () => {
      if (modal.open) modal.close();
      document.body.style.overflow = previousOverflow;
    };
  }, [open, isMobile]);

  useEffect(() => {
    if (open && !isMobile && focusFirstOptionOnOpen.current) {
      firstOptionRef.current?.focus();
      focusFirstOptionOnOpen.current = false;
    }
  }, [open, isMobile]);

  function openMenu() {
    setDraftValues(selectedValues);
    setDraftText(textFilter?.value ?? '');
    if (!isMobile) {
      const bounds = wrapperRef.current?.getBoundingClientRect();
      const estimatedHeight = Math.min(170 + options.length * 42 + (textFilter ? 70 : 0), 480);
      const spaceBelow = bounds ? window.innerHeight - bounds.bottom : 0;
      setOpenUp(Boolean(bounds && spaceBelow < estimatedHeight && bounds.top > spaceBelow));
    }
    setOpen(true);
  }

  function toggleOption(value) {
    setDraftValues((current) => current.includes(value)
      ? current.filter((selected) => selected !== value)
      : [...current, value]);
  }

  function applyFilters() {
    onChange(draftValues, draftText.trim());
    closeAndFocus();
  }

  function resetFilters() {
    const initialValues = options.map((option) => option.value);
    setDraftValues(initialValues);
    setDraftText('');
    onChange(initialValues, '');
    onReset?.();
    closeAndFocus();
  }

  function closeAndFocus() {
    setOpen(false);
    window.requestAnimationFrame(() => triggerRef.current?.focus());
  }

  const filterControls = (
    <>
      {textFilter && (
        <label className="filter__text-field">
          <span>{textFilter.label}</span>
          <input
            type="search"
            value={draftText}
            placeholder={textFilter.placeholder}
            onChange={(event) => setDraftText(event.target.value)}
          />
        </label>
      )}
      <div className="filter__options-title">{optionsLabel}</div>
      <button
        type="button"
        className="filter__select-all"
        onClick={() => setDraftValues(options.map((option) => option.value))}
      >
        Ver todas
      </button>
      <div className="filter__options" role="group" aria-label={optionsLabel}>
        {options.map((option, index) => (
          <label key={option.value} className="filter__option">
            <input
              ref={index === 0 ? firstOptionRef : undefined}
              type="checkbox"
              checked={draftValues.includes(option.value)}
              onChange={() => toggleOption(option.value)}
            />
            <span>{option.label}</span>
          </label>
        ))}
      </div>
      <div className="filter__footer">
        <button type="button" className="filter__reset" onClick={resetFilters}>Resetear</button>
        <button type="button" className="filter__apply" onClick={applyFilters}>Aplicar</button>
      </div>
    </>
  );

  return (
    <div
      ref={wrapperRef}
      className={`filter${open ? ' filter--open' : ''}${openUp ? ' filter--open-up' : ''}`}
      onBlurCapture={(event) => {
        if (!isMobile && event.relatedTarget && !event.currentTarget.contains(event.relatedTarget)) {
          setOpen(false);
        }
      }}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open && !isMobile) closeAndFocus();
      }}
    >
      <button
        ref={triggerRef}
        type="button"
        className="filter__trigger"
        aria-expanded={open}
        aria-controls={menuId}
        aria-haspopup={isMobile ? 'dialog' : undefined}
        aria-label={`${label}: ${selectedLabel}${appliedText ? `, etiqueta ${appliedText}` : ''}`}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            if (open) firstOptionRef.current?.focus();
            else {
              focusFirstOptionOnOpen.current = !isMobile;
              openMenu();
            }
          }
        }}
        onClick={() => {
          if (open) setOpen(false);
          else openMenu();
        }}
      >
        <IoFilter className="filter__icon" aria-hidden="true" />
        <span className="filter__value">{label}</span>
        {hasActiveFilters && <span className="filter__active-indicator" aria-hidden="true" />}
      </button>

      {open && (isMobile ? (
        <dialog
          ref={modalRef}
          id={menuId}
          className="filter__modal"
          aria-labelledby={modalTitleId}
          aria-modal="true"
          onCancel={(event) => {
            event.preventDefault();
            closeAndFocus();
          }}
        >
          <div className="filter__mobile-header">
            <h2 id={modalTitleId}>{label}</h2>
            <button ref={mobileCloseRef} type="button" aria-label="Cerrar filtros" onClick={closeAndFocus}>
              <MdClose aria-hidden="true" />
            </button>
          </div>
          {filterControls}
        </dialog>
      ) : (
        <div id={menuId} className="filter__menu" role="group" aria-label={label}>
          {filterControls}
        </div>
      ))}
    </div>
  );
}
