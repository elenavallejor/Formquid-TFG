import { useEffect, useId, useRef, useState } from 'react';
import { BsChevronDown } from 'react-icons/bs';

import {
  CATEGORY_ICON_COMPONENTS,
  CATEGORY_ICON_OPTIONS,
  DEFAULT_CATEGORY_ICON,
} from '../../utils/categoryIconOptions.js';

import './CategoryIconPicker.css';

export function CategoryIconPicker({ value, onChange, label, compact = false, disabled = false }) {
  const [open, setOpen] = useState(false);
  const [openUp, setOpenUp] = useState(false);
  const [menuLeft, setMenuLeft] = useState(0);
  const [menuMaxHeight, setMenuMaxHeight] = useState(380);
  const wrapperRef = useRef(null);
  const triggerRef = useRef(null);
  const menuId = useId();
  const selectedIcon = CATEGORY_ICON_COMPONENTS[value] ? value : DEFAULT_CATEGORY_ICON;
  const SelectedIcon = CATEGORY_ICON_COMPONENTS[selectedIcon];
  const selectedLabel = CATEGORY_ICON_OPTIONS.find((option) => option.value === selectedIcon)?.label;

  useEffect(() => {
    if (!open) return undefined;

    function handlePointerDown(event) {
      if (!wrapperRef.current?.contains(event.target)) setOpen(false);
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  function toggleMenu() {
    if (!open) {
      const bounds = triggerRef.current?.getBoundingClientRect();
      if (bounds) {
        const menuWidth = Math.min(440, window.innerWidth - 32);
        const left = Math.max(
          16 - bounds.left,
          Math.min(0, window.innerWidth - 16 - menuWidth - bounds.left),
        );
        const spaceBelow = window.innerHeight - bounds.bottom - 16;
        const spaceAbove = bounds.top - 16;
        const above = spaceBelow < 300 && spaceAbove > spaceBelow;
        setMenuLeft(left);
        setOpenUp(above);
        setMenuMaxHeight(Math.max(120, Math.min(380, above ? spaceAbove : spaceBelow)));
      }
    }
    setOpen((current) => !current);
  }

  function selectIcon(icon) {
    onChange(icon);
    setOpen(false);
    triggerRef.current?.focus();
  }

  return (
    <div
      className={`categories-icon-picker${compact ? ' categories-icon-picker--compact' : ''}${openUp ? ' categories-icon-picker--up' : ''}`}
      ref={wrapperRef}
    >
      <button
        ref={triggerRef}
        type="button"
        className="categories-icon-trigger"
        aria-label={`${label}: ${selectedLabel}`}
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        disabled={disabled}
        onClick={toggleMenu}
      >
        <SelectedIcon aria-hidden="true" />
        {!compact && <span>{selectedLabel}</span>}
        <BsChevronDown className="categories-icon-chevron" aria-hidden="true" />
      </button>

      {open && (
        <div
          className="categories-icon-menu"
          id={menuId}
          role="group"
          aria-label="Iconos disponibles"
          style={{ left: menuLeft, maxHeight: menuMaxHeight }}
        >
          <div className="categories-icon-grid">
            {CATEGORY_ICON_OPTIONS.map(({ value: optionValue, label: optionLabel, Icon }) => (
              <button
                key={optionValue}
                type="button"
                className="categories-icon-option"
                aria-label={optionLabel}
                aria-pressed={selectedIcon === optionValue}
                onClick={() => selectIcon(optionValue)}
              >
                <Icon aria-hidden="true" />
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
