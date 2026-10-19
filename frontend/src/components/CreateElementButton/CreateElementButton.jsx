import { useRef, useState } from "react";
import { MdAdd, MdClose, MdHelpOutline, MdViewModule } from "react-icons/md";
import { IoMdText } from "react-icons/io";

import { useModalFocus } from "../../hooks/useModalFocus.js";
import { CREATABLE_ELEMENT_TYPE, ELEMENT_CREATOR_CONTEXT } from "./createElementOptions.js";
import "./CreateElementButton.css";

/**
 * Muestra un botón reutilizable para elegir qué tipo de elemento crear en cada constructor.
 */
const BASE_OPTIONS = [
  {
    type: CREATABLE_ELEMENT_TYPE.TEXT,
    title: "Texto",
    description: "Añade una introducción, explicación o indicación.",
    icon: IoMdText,
  },
  {
    type: CREATABLE_ELEMENT_TYPE.QUESTION,
    title: "Pregunta nueva",
    description: "Crea y configura una pregunta propia desde cero.",
    icon: MdHelpOutline,
  },
];

const BLOCK_OPTION = {
  type: CREATABLE_ELEMENT_TYPE.BLOCK,
  title: "Bloque nuevo",
  description: "Crea un bloque con su propia estructura de preguntas y textos.",
  icon: MdViewModule,
};

export function CreateElementButton({
  context = ELEMENT_CREATOR_CONTEXT.BLOCK,
  onSelect,
  disabled = false,
  className = "",
}) {
  const [isOpen, setIsOpen] = useState(false);
  const firstOptionRef = useRef(null);
  const modalRef = useRef(null);
  const triggerRef = useRef(null);
  const allowsBlocks =
    context === ELEMENT_CREATOR_CONTEXT.TEMPLATE ||
    context === ELEMENT_CREATOR_CONTEXT.SURVEY;
  const options = allowsBlocks ? [...BASE_OPTIONS, BLOCK_OPTION] : BASE_OPTIONS;

  const { preventFocusRestore } = useModalFocus({
    isOpen,
    onClose: closeModal,
    dialogRef: modalRef,
    initialFocusRef: firstOptionRef,
    returnFocusRef: triggerRef,
  });

  function closeModal() {
    setIsOpen(false);
    window.requestAnimationFrame(() => triggerRef.current?.focus());
  }

  function handleSelect(type) {
    preventFocusRestore();
    setIsOpen(false);
    onSelect?.(type);
  }

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className={`create-element-trigger ${className}`.trim()}
        disabled={disabled}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        onClick={() => setIsOpen(true)}
      >
        <MdAdd aria-hidden="true" />
        <span>Crear elemento</span>
      </button>

      {isOpen && (
        <div
          className="create-element-overlay"
          onMouseDown={(event) =>
            event.target === event.currentTarget && closeModal()
          }
        >
          <section
            ref={modalRef}
            tabIndex={-1}
            className="create-element-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-element-title"
          >
            <header className="create-element-header">
              <div>
                <h2 id="create-element-title">CREAR ELEMENTO</h2>
                <p>Selecciona el tipo de contenido que quieres añadir.</p>
              </div>
              <button
                type="button"
                className="create-element-close"
                aria-label="Cerrar selector"
                onClick={closeModal}
              >
                <MdClose aria-hidden="true" />
              </button>
            </header>

            <div className="create-element-options">
              {options.map((option, index) => {
                const Icon = option.icon;
                return (
                  <button
                    key={option.type}
                    ref={index === 0 ? firstOptionRef : null}
                    type="button"
                    className="create-element-option"
                    onClick={() => handleSelect(option.type)}
                  >
                    <span className="create-element-option-icon">
                      <Icon aria-hidden="true" />
                    </span>
                    <span className="create-element-option-content">
                      <strong>{option.title}</strong>
                      <span>{option.description}</span>
                    </span>
                  </button>
                );
              })}
            </div>
          </section>
        </div>
      )}
    </>
  );
}
