import { Children, isValidElement, useEffect, useId, useMemo, useRef, useState } from 'react';
import { MdKeyboardArrowDown } from 'react-icons/md';

import './Dropdown.css';

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
  const buttonId = id || `dropdown-${generatedId}`;
  const menuId = `${buttonId}-menu`;
  const rootRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);
  const [opensUpward, setOpensUpward] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);

  const options = useMemo(
    () =>
      Children.toArray(children)
        .filter(isValidElement)
        .map((option) => ({
          disabled: Boolean(option.props.disabled),
          label: option.props.children,
          style: option.props.style,
          value: String(option.props.value ?? ''),
        })),
    [children],
  );

  const selectedIndex = Math.max(
    0,
    options.findIndex((option) => option.value === String(value ?? '')),
  );
  const selectedOption = options[selectedIndex];

  useEffect(() => {
    function handlePointerDown(event) {
      if (!rootRef.current?.contains(event.target)) setIsOpen(false);
    }

    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, []);

  function openMenu(index = selectedIndex) {
    if (disabled || options.length === 0) return;
    const bounds = rootRef.current?.getBoundingClientRect();
    const menuHeight = Math.min(options.length * 40 + 2, 320);
    setOpensUpward(
      Boolean(
        bounds &&
          window.innerHeight - bounds.bottom < menuHeight + 6 &&
          bounds.top > menuHeight + 6,
      ),
    );
    setActiveIndex(index);
    setIsOpen(true);
  }

  function selectOption(option) {
    if (!option || option.disabled) return;
    onChange?.({ target: { name, value: option.value } });
    setIsOpen(false);
  }

  function moveActiveIndex(direction) {
    if (options.length === 0) return;

    let nextIndex = activeIndex;
    do {
      nextIndex = (nextIndex + direction + options.length) % options.length;
    } while (options[nextIndex].disabled && nextIndex !== activeIndex);

    setActiveIndex(nextIndex);
  }

  function handleKeyDown(event) {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (isOpen) moveActiveIndex(1);
        else openMenu();
        break;
      case 'ArrowUp':
        event.preventDefault();
        if (isOpen) moveActiveIndex(-1);
        else openMenu();
        break;
      case 'Home':
        if (isOpen) {
          event.preventDefault();
          setActiveIndex(0);
        }
        break;
      case 'End':
        if (isOpen) {
          event.preventDefault();
          setActiveIndex(options.length - 1);
        }
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        if (isOpen) selectOption(options[activeIndex]);
        else openMenu();
        break;
      case 'Escape':
        if (isOpen) {
          event.preventDefault();
          setIsOpen(false);
        }
        break;
      case 'Tab':
        setIsOpen(false);
        break;
      default:
        break;
    }
  }

  return (
    <div
      ref={rootRef}
      className={`custom-dropdown ${isOpen ? 'is-open' : ''} ${opensUpward ? 'opens-upward' : ''} ${className}`.trim()}
      onMouseLeave={() => setIsOpen(false)}
    >
      <button
        id={buttonId}
        type="button"
        className="custom-dropdown-trigger"
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedBy}
        aria-invalid={ariaInvalid}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={isOpen ? menuId : undefined}
        aria-activedescendant={isOpen ? `${menuId}-option-${activeIndex}` : undefined}
        disabled={disabled}
        onClick={() => (isOpen ? setIsOpen(false) : openMenu())}
        onKeyDown={handleKeyDown}
      >
        <span style={selectedOption?.style}>{selectedOption?.label}</span>
        <MdKeyboardArrowDown aria-hidden="true" />
      </button>

      {isOpen && (
        <div
          id={menuId}
          className={`custom-dropdown-menu ${opensUpward ? 'opens-upward' : ''}`.trim()}
          role="listbox"
          aria-labelledby={buttonId}
        >
          {options.map((option, index) => (
            <button
              id={`${menuId}-option-${index}`}
              key={option.value}
              type="button"
              role="option"
              className={`custom-dropdown-option ${index === activeIndex ? 'active' : ''}`.trim()}
              style={option.style}
              aria-selected={option.value === String(value ?? '')}
              disabled={option.disabled}
              tabIndex={-1}
              onClick={() => selectOption(option)}
              onMouseEnter={() => setActiveIndex(index)}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
