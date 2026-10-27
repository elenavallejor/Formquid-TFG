import { Children, useEffect, useId, useRef, useState } from 'react';
import { MdCheck, MdKeyboardArrowDown } from 'react-icons/md';

import './Dropdown.css';

/**
 * Selector compartido de FormQuid. Su menú no bloquea la página y se cierra
 * al salir con el cursor, pulsar fuera o abandonar el control con el teclado.
 */
export function Dropdown({
  children,
  className = '',
  disabled = false,
  id,
  name,
  onChange,
  value,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
  'aria-invalid': ariaInvalid,
}) {
  const generatedId = useId().replaceAll(':', '');
  const selectId = id || `dropdown-${generatedId}`;

  return (
    <DropdownPopover
      ariaDescribedBy={ariaDescribedBy}
      ariaInvalid={ariaInvalid}
      ariaLabel={ariaLabel}
      className={className}
      disabled={disabled}
      id={selectId}
      name={name}
      onChange={onChange}
      options={Children.toArray(children)}
      value={value}
    />
  );
}

function DropdownPopover({ ariaDescribedBy, ariaInvalid, ariaLabel, className, disabled, id, name, onChange, options, value }) {
  const [open, setOpen] = useState(false);
  const [openUp, setOpenUp] = useState(false);
  const wrapperRef = useRef(null);
  const triggerRef = useRef(null);
  const optionRefs = useRef([]);
  const focusOptionOnOpen = useRef(false);
  const menuId = `${id}-menu`;
  const selectedIndex = options.findIndex((option) => String(option.props.value) === String(value));
  const selectedOption = options[selectedIndex] ?? options[0];
  const enabledIndexes = options.flatMap((option, index) => option.props.disabled ? [] : [index]);
  const focusIndex = enabledIndexes.includes(selectedIndex) ? selectedIndex : enabledIndexes[0];

  useEffect(() => {
    if (!open) return undefined;

    function handlePointerDown(event) {
      if (!wrapperRef.current?.contains(event.target)) setOpen(false);
    }

    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [open]);

  useEffect(() => {
    if (open && focusOptionOnOpen.current) {
      optionRefs.current[focusIndex]?.focus();
      focusOptionOnOpen.current = false;
    }
  }, [open, focusIndex]);

  function openMenu() {
    const bounds = wrapperRef.current?.getBoundingClientRect();
    const menuHeight = Math.min(options.length * 44 + 8, 280);
    const spaceBelow = bounds ? window.innerHeight - bounds.bottom : 0;
    setOpenUp(Boolean(bounds && spaceBelow < menuHeight && bounds.top > spaceBelow));
    setOpen(true);
  }

  function handleBlur(event) {
    if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape' && open) {
      setOpen(false);
      triggerRef.current?.focus();
    }
  }

  function handleTriggerKeyDown(event) {
    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
    event.preventDefault();
    if (open) {
      optionRefs.current[focusIndex]?.focus();
    } else {
      focusOptionOnOpen.current = true;
      openMenu();
    }
  }

  function handleOptionKeyDown(event, index) {
    if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault();
      optionRefs.current[event.key === 'Home' ? enabledIndexes[0] : enabledIndexes.at(-1)]?.focus();
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const direction = event.key === 'ArrowDown' ? 1 : -1;
      const nextIndex = (enabledIndexes.indexOf(index) + direction + enabledIndexes.length) % enabledIndexes.length;
      optionRefs.current[enabledIndexes[nextIndex]]?.focus();
    }
  }

  return (
    <div
      ref={wrapperRef}
      className={`custom-dropdown ${openUp ? 'custom-dropdown--open-up' : ''} ${className}`.trim()}
      onMouseLeave={() => setOpen(false)}
      onBlurCapture={handleBlur}
      onKeyDown={handleKeyDown}
    >
      <button
        ref={triggerRef}
        id={id}
        type="button"
        className="custom-dropdown-trigger"
        aria-label={ariaLabel ? `${ariaLabel}: ${selectedOption?.props.children ?? 'Seleccionar'}` : undefined}
        aria-describedby={ariaDescribedBy}
        aria-invalid={ariaInvalid}
        aria-expanded={open && !disabled}
        aria-controls={open && !disabled ? menuId : undefined}
        disabled={disabled}
        onKeyDown={handleTriggerKeyDown}
        onClick={() => {
          if (open) setOpen(false);
          else openMenu();
        }}
      >
        {selectedOption?.props.children ?? 'Seleccionar'}
      </button>
      <MdKeyboardArrowDown className="custom-dropdown-icon" aria-hidden="true" />
      {name && <input type="hidden" name={name} value={value ?? ''} disabled={disabled} />}
      {open && !disabled && (
        <div id={menuId} className="custom-dropdown-menu" role="group" aria-label={ariaLabel}>
          {options.map((option, index) => (
            <button
              key={option.key ?? option.props.value}
              ref={(element) => { optionRefs.current[index] = element; }}
              type="button"
              className="custom-dropdown-option"
              aria-pressed={String(value) === String(option.props.value)}
              disabled={option.props.disabled}
              style={option.props.style}
              onKeyDown={(event) => handleOptionKeyDown(event, index)}
              onClick={() => {
                onChange?.({ target: { name, value: String(option.props.value) } });
                setOpen(false);
                triggerRef.current?.focus();
              }}
            >
              <span>{option.props.children}</span>
              {String(value) === String(option.props.value) && (
                <MdCheck aria-hidden="true" />
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
