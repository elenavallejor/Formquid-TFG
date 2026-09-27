import { useRef, useState } from "react";
import { MdAdd, MdClose, MdDeleteOutline, MdDragIndicator, MdEdit, MdKeyboardArrowDown, MdKeyboardArrowUp, MdMoreVert } from "react-icons/md";
import { useModalFocus } from "../../hooks/useModalFocus.js";
import { buildBlockQuestionFromCatalog, buildBlockTextItem, getBlockItems, INITIAL_BLOCK_STATE } from "../../utils/blockBuilderUtils.js";
import { getDragData, getDropIndex, reorderItems } from "../../utils/builderDragUtils.js";
import { INITIAL_QUESTION_STATE, QUESTION_TYPES } from "../../utils/questionBuilderUtils.js";
import { CREATABLE_ELEMENT_TYPE, CreateElementButton, ELEMENT_CREATOR_CONTEXT } from "../CreateElementButton/CreateElementButton.jsx";
import { CatalogSidebar } from "../CatalogSidebar/CatalogSidebar.jsx";
import { ErrorMessage } from "../ErrorMessage/ErrorMessage.jsx";
import { Question } from "../Question/Question.jsx";
import { QuestionBuilder } from "../QuestionBuilder/QuestionBuilder.jsx";
import { RequiredToggle } from "../RequiredToggle/RequiredToggle.jsx";
import { TextContent, TextContentIcon } from "../TextContent/TextContent.jsx";
import "./BlockBuilder.css";

/**
 * Gestiona preguntas y textos de un bloque utilizando las utilidades compartidas de arrastre.
 */
export function BlockBuilder({
  data = INITIAL_BLOCK_STATE,
  onChange = () => {},
  onDropQuestionFromSidebar,
  isModal = false,
  isOpen = true,
  onClose,
  onSave,
  modalTitle = "Configurar bloque",
  errorMessage = "",
  onClearError,
}) {
  const [activeMenuIndex, setActiveMenuIndex] = useState(null);
  const [modalMode, setModalMode] = useState(null);
  const [selectedItemIndex, setSelectedItemIndex] = useState(null);
  const [editingQuestion, setEditingQuestion] = useState(INITIAL_QUESTION_STATE);
  const [editingText, setEditingText] = useState("");
  const [modalError, setModalError] = useState("");
  const [dropIndex, setDropIndex] = useState(null);
  const [reorderMessage, setReorderMessage] = useState("");
  const outerModalRef = useRef(null);
  const outerModalTitleRef = useRef(null);
  const textModalRef = useRef(null);
  const textInputRef = useRef(null);
  const deleteModalRef = useRef(null);
  const deleteCancelRef = useRef(null);

  const items = getBlockItems(data);
  const selectedItem = selectedItemIndex === null ? null : items[selectedItemIndex];
  let questionCount = 0;
  const questionNumberByIndex = items.map((item) =>
    item.itemType !== "TEXT" ? ++questionCount : null,
  );

  useModalFocus({
    isOpen: isModal && isOpen,
    onClose,
    dialogRef: outerModalRef,
    initialFocusRef: outerModalTitleRef,
  });
  useModalFocus({
    isOpen: modalMode === "CREATE_TEXT" || modalMode === "EDIT_TEXT",
    onClose: closeModal,
    dialogRef: textModalRef,
    initialFocusRef: textInputRef,
  });
  useModalFocus({
    isOpen: modalMode === "DELETE" && Boolean(selectedItem),
    onClose: closeModal,
    dialogRef: deleteModalRef,
    initialFocusRef: deleteCancelRef,
  });

  function updateData(changes) {
    const currentData = { ...data };
    delete currentData.questions;
    onChange({ ...currentData, ...changes });
  }

  function updateItems(nextItems) {
    updateData({ items: nextItems });
  }

  function preventDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = "none";
  }

  function handleDragStart(event, index) {
    event.dataTransfer.setData("text/plain", JSON.stringify({ type: "INTERNAL_SORT", index }));
    event.dataTransfer.effectAllowed = "move";
  }

  function handleCatalogQuestion(question, targetIndex) {
    if (onDropQuestionFromSidebar) {
      onDropQuestionFromSidebar(question, targetIndex);
      return;
    }

    const updated = [...items];
    const insertionIndex = Number.isInteger(targetIndex)
      ? Math.max(0, Math.min(targetIndex, updated.length))
      : updated.length;
    updated.splice(insertionIndex, 0, buildBlockQuestionFromCatalog(question));
    updateItems(updated);
  }

  function moveItem(fromIndex, toIndex) {
    const updated = reorderItems(items, fromIndex, toIndex);
    if (updated === items) return;

    updateItems(updated);
    setReorderMessage("Orden de los elementos actualizado.");
  }

  function handleItemDragOver(event, itemIndex) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === "move" ? "move" : "copy";
    setDropIndex(getDropIndex(event, itemIndex));
  }

  function handleDropAt(event, targetIndex) {
    event.preventDefault();
    event.stopPropagation();
    setDropIndex(null);

    const dragData = getDragData(event);

    if (dragData?.type === "CATALOG_QUESTION") {
      handleCatalogQuestion(dragData.question, targetIndex);
    } else if (dragData?.type === "INTERNAL_SORT") {
      moveItem(dragData.index, targetIndex);
    }
  }

  function handleItemDrop(event, itemIndex) {
    handleDropAt(event, getDropIndex(event, itemIndex));
  }

  function handleStructureDrop(event) {
    handleDropAt(event, items.length);
  }

  function handleReorderKeyDown(event, index) {
    if (event.key !== "ArrowUp" && event.key !== "ArrowDown") return;
    event.preventDefault();
    moveItem(index, event.key === "ArrowUp" ? index - 1 : index + 2);
  }

  function handleRequiredChange(index, isRequired) {
    const updated = [...items];
    updated[index] = { ...updated[index], isRequired };
    updateItems(updated);
  }

  function openCreateQuestionModal() {
    setEditingQuestion({
      ...INITIAL_QUESTION_STATE,
      itemType: "QUESTION",
      isRequired: false,
    });
    setSelectedItemIndex(null);
    setModalError("");
    setModalMode("CREATE_QUESTION");
  }

  function openCreateTextModal() {
    setEditingText("");
    setSelectedItemIndex(null);
    setModalError("");
    setModalMode("CREATE_TEXT");
  }

  function handleCreateElement(type) {
    if (type === CREATABLE_ELEMENT_TYPE.TEXT) openCreateTextModal();
    if (type === CREATABLE_ELEMENT_TYPE.QUESTION) openCreateQuestionModal();
  }

  function openItemModal(mode, index) {
    const item = items[index];
    setSelectedItemIndex(index);
    setActiveMenuIndex(null);
    setModalError("");

    if (mode === "EDIT_QUESTION") setEditingQuestion({ ...item });
    if (mode === "EDIT_TEXT") setEditingText(item.textBody || "");

    setModalMode(mode);
  }

  function closeModal() {
    setModalMode(null);
    setSelectedItemIndex(null);
    setEditingQuestion(INITIAL_QUESTION_STATE);
    setEditingText("");
    setModalError("");
  }

  function handleSaveQuestion() {
    const question = { ...editingQuestion, itemType: "QUESTION" };

    if (modalMode === "CREATE_QUESTION") {
      updateItems([...items, question]);
    } else if (modalMode === "EDIT_QUESTION" && selectedItemIndex !== null) {
      const updated = [...items];
      updated[selectedItemIndex] = question;
      updateItems(updated);
    }

    closeModal();
  }

  function handleSaveText() {
    const normalizedText = editingText.trim();

    if (!normalizedText) {
      setModalError("El texto no puede estar vacío.");
      return;
    }

    if (modalMode === "CREATE_TEXT") {
      updateItems([...items, buildBlockTextItem(normalizedText)]);
    } else if (modalMode === "EDIT_TEXT" && selectedItemIndex !== null) {
      const updated = [...items];
      updated[selectedItemIndex] = {
        ...updated[selectedItemIndex],
        textBody: normalizedText,
      };
      updateItems(updated);
    }

    closeModal();
  }

  function handleDeleteItem() {
    if (selectedItemIndex === null) return;
    updateItems(items.filter((_, index) => index !== selectedItemIndex));
    closeModal();
  }

  const builderContent = (
    <div className={`block-builder-stack ${isModal ? "modal-mode" : ""}`}>
      <section className="builder-card" onDragOverCapture={preventDrop} onDropCapture={preventDrop}>
        <h2 className="builder-section-title">INFORMACIÓN GENERAL</h2>

        <div className="builder-field">
          <label htmlFor="block-name">
            Nombre del bloque <span>*</span>
          </label>
          <input
            id="block-name"
            type="text"
            placeholder="Introduce un nombre descriptivo para el bloque..."
            maxLength={150}
            value={data.name || ""}
            onChange={(event) => updateData({ name: event.target.value })}
          />
          <span className="field-counter">{(data.name || "").length}/150</span>
        </div>

        <div className="builder-field">
          <label htmlFor="block-description">Descripción</label>
          <textarea
            id="block-description"
            rows={4}
            placeholder="Añade detalles o contexto sobre el propósito de este bloque..."
            maxLength={500}
            value={data.description || ""}
            onChange={(event) => updateData({ description: event.target.value })}
          />
          <span className="field-counter">{(data.description || "").length}/500</span>
        </div>
      </section>

      <section
        className="builder-card block-structure-card responsive-builder-structure-card"
        onDragOver={(event) => event.preventDefault()}
        onDrop={handleStructureDrop}
        onDragLeave={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget)) setDropIndex(null);
        }}
      >
        <div className="structure-header">
          <div>
            <h2 className="builder-section-title">ESTRUCTURA DEL BLOQUE</h2>
            <p className="builder-section-desc">
              Combina preguntas y textos, y ordénalos como aparecerán.
            </p>
          </div>
          <CreateElementButton context={ELEMENT_CREATOR_CONTEXT.BLOCK} onSelect={handleCreateElement} />
        </div>

        <div className="block-questions-list">
          <p className="sr-only" aria-live="polite">{reorderMessage}</p>

          {items.map((item, index) => {
            const isText = item.itemType === "TEXT";
            const isMenuOpen = activeMenuIndex === index;
            const typeConfig = isText
              ? null
              : QUESTION_TYPES.find((type) => type.id === item.questionType);
            const TypeIcon = typeConfig?.icon;

            return (
              <div
                key={item.localId || item.id || index}
                className={`block-question-item ${dropIndex === index ? "drop-before" : ""} ${index === items.length - 1 && dropIndex === items.length ? "drop-after" : ""}`}
                onDragOver={(event) => handleItemDragOver(event, index)}
                onDrop={(event) => handleItemDrop(event, index)}
              >
                <div className="question-item-left">
                  <button
                    type="button"
                    className="drag-handle"
                    draggable
                    aria-label={`Reordenar elemento ${index + 1}. Usa las flechas arriba y abajo o arrastra el control.`}
                    title="Arrastra o usa las flechas para reordenar"
                    onDragStart={(event) => handleDragStart(event, index)}
                    onDragEnd={() => setDropIndex(null)}
                    onKeyDown={(event) => handleReorderKeyDown(event, index)}
                  >
                    <MdDragIndicator aria-hidden="true" />
                  </button>

                  {isText ? (
                    <TextContentIcon />
                  ) : TypeIcon ? (
                    <span
                      className="question-type-badge"
                      style={{ color: typeConfig.color, borderColor: `${typeConfig.color}60` }}
                      title={typeConfig.title}
                    >
                      <TypeIcon aria-hidden="true" />
                    </span>
                  ) : null}
                </div>

                <div className="question-item-preview">
                  {isText ? (
                    <TextContent data={item} />
                  ) : (
                    <Question questionNumber={`${questionNumberByIndex[index]}`} data={item} interactive={false} />
                  )}
                </div>

                <div className="question-item-actions">
                  <button
                    type="button"
                    className="action-dots-btn"
                    aria-label={`Mover ${isText ? "texto" : "pregunta"} ${index + 1} hacia arriba`}
                    disabled={index === 0}
                    onClick={() => moveItem(index, index - 1)}
                  >
                    <MdKeyboardArrowUp aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    className="action-dots-btn"
                    aria-label={`Mover ${isText ? "texto" : "pregunta"} ${index + 1} hacia abajo`}
                    disabled={index === items.length - 1}
                    onClick={() => moveItem(index, index + 2)}
                  >
                    <MdKeyboardArrowDown aria-hidden="true" />
                  </button>
                  {!isText && (
                    <RequiredToggle
                      checked={item.isRequired}
                      onChange={(isRequired) => handleRequiredChange(index, isRequired)}
                    />
                  )}
                  <button
                    type="button"
                    className="action-dots-btn"
                    aria-label={`Opciones de ${isText ? "texto" : "pregunta"}`}
                    onClick={() => setActiveMenuIndex(isMenuOpen ? null : index)}
                  >
                    <MdMoreVert aria-hidden="true" />
                  </button>

                  {isMenuOpen && (
                    <div className="actions-floating-menu">
                      <button type="button" onClick={() => openItemModal(isText ? "EDIT_TEXT" : "EDIT_QUESTION", index)}>
                        <MdEdit aria-hidden="true" />
                        Editar {isText ? "texto" : "pregunta"}
                      </button>
                      <button type="button" className="delete-opt" onClick={() => openItemModal("DELETE", index)}>
                        <MdDeleteOutline aria-hidden="true" />
                        Eliminar del bloque
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div
          className={`block-empty-dropzone ${dropIndex === items.length ? "drop-active" : ""}`}
          onDragOver={(event) => {
            event.preventDefault();
            event.stopPropagation();
            event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === "move" ? "move" : "copy";
            setDropIndex(items.length);
          }}
          onDrop={handleStructureDrop}
        >
          <div className="dropzone-text-group">
            <span className="dropzone-title">
              <MdAdd aria-hidden="true" />
              Agrega contenido
            </span>
            <p className="dropzone-sub">
              Arrastra preguntas desde el catálogo o utiliza el botón «Crear elemento».
            </p>
          </div>
        </div>
      </section>

      {(modalMode === "CREATE_QUESTION" || modalMode === "EDIT_QUESTION") && (
        <QuestionBuilder
          data={editingQuestion}
          onChange={setEditingQuestion}
          isCatalog={false}
          isModal
          modalTitle={modalMode === "CREATE_QUESTION" ? "Crear pregunta propia" : "Editar pregunta"}
          onClose={closeModal}
          onSave={handleSaveQuestion}
        />
      )}

      {(modalMode === "CREATE_TEXT" || modalMode === "EDIT_TEXT") && (
        <div className="block-modal-overlay" onClick={closeModal}>
          <section
            ref={textModalRef}
            tabIndex={-1}
            className="block-modal-container text-editor-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="text-editor-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="block-modal-header">
              <h2 id="text-editor-title">
                {modalMode === "CREATE_TEXT" ? "AÑADIR TEXTO" : "EDITAR TEXTO"}
              </h2>
              <button type="button" className="btn-close-modal" aria-label="Cerrar modal" onClick={closeModal}>
                <MdClose aria-hidden="true" />
              </button>
            </header>

            <div className="block-modal-body">
              <div className="builder-field">
                <label htmlFor="block-text-body">
                  Contenido <span>*</span>
                </label>
                <textarea
                  ref={textInputRef}
                  id="block-text-body"
                  rows={8}
                  maxLength={5000}
                  placeholder="Escribe una introducción, explicación o indicación para este apartado..."
                  value={editingText}
                  onChange={(event) => {
                    setEditingText(event.target.value);
                    setModalError("");
                  }}
                />
                <span className="field-counter">{editingText.length}/5000</span>
              </div>

              <ErrorMessage message={modalError} onClose={() => setModalError("")} />
            </div>

            <footer className="block-modal-footer">
              <button type="button" className="btn-cancel" onClick={closeModal}>
                Cancelar
              </button>
              <button type="button" className="btn-confirm" onClick={handleSaveText}>
                Guardar texto
              </button>
            </footer>
          </section>
        </div>
      )}

      {modalMode === "DELETE" && selectedItem && (
        <div className="block-modal-overlay" onClick={closeModal}>
          <section
            ref={deleteModalRef}
            tabIndex={-1}
            className="block-modal-container delete-modal-box"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-item-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="block-modal-header">
              <h2 id="delete-item-title">
                ELIMINAR {selectedItem.itemType === "TEXT" ? "TEXTO" : "PREGUNTA"}
              </h2>
              <button type="button" className="btn-close-modal" aria-label="Cerrar modal" onClick={closeModal}>
                <MdClose aria-hidden="true" />
              </button>
            </header>
            <div className="block-modal-body">
              <p>¿Estás seguro de que deseas retirar este elemento del bloque?</p>
            </div>
            <footer className="block-modal-footer">
              <button ref={deleteCancelRef} type="button" className="btn-cancel" onClick={closeModal}>
                Cancelar
              </button>
              <button type="button" className="btn-danger-confirm" onClick={handleDeleteItem}>
                Eliminar
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );

  if (!isModal) return builderContent;
  if (!isOpen) return null;

  return (
    <div className="block-modal-overlay" onClick={onClose}>
      <section
        ref={outerModalRef}
        tabIndex={-1}
        className="block-modal-container block-builder-modal-wrapper"
        role="dialog"
        aria-modal="true"
        aria-labelledby="block-builder-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="block-modal-header">
          <h2 ref={outerModalTitleRef} id="block-builder-title" tabIndex={-1}>{modalTitle}</h2>
          {onClose && (
            <button type="button" className="btn-close-modal" aria-label="Cerrar modal" onClick={onClose}>
              <MdClose aria-hidden="true" />
            </button>
          )}
        </header>
        <div className="block-modal-body block-builder-modal-body">
          <CatalogSidebar
            embedded
            showQuestions
            showBlocks={false}
            onAddQuestion={handleCatalogQuestion}
          />
          <div className="block-builder-modal-content">
            {builderContent}
            <div className="block-modal-error-message">
              <ErrorMessage message={errorMessage} onClose={onClearError} />
            </div>
          </div>
        </div>
        {(onClose || onSave) && (
          <footer className="block-modal-footer">
            {onClose && (
              <button type="button" className="btn-cancel" onClick={onClose}>
                Cancelar
              </button>
            )}
            {onSave && (
              <button type="button" className="btn-confirm" onClick={onSave}>
                Guardar bloque
              </button>
            )}
          </footer>
        )}
      </section>
    </div>
  );
}
