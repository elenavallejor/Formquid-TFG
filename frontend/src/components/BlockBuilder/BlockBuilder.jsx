import { useState } from 'react';
import { MdAdd, MdClose, MdDeleteOutline, MdDragIndicator, MdEdit, MdMoreVert, MdOutlineVisibility } from 'react-icons/md';
import { INITIAL_QUESTION_STATE, QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { Question } from '../Question/Question.jsx';
import { QuestionBuilder } from '../QuestionBuilder/QuestionBuilder.jsx';

import './BlockBuilder.css';

/**
 * Permite configurar un bloque, insertar preguntas en cualquier posición y reorganizarlas.
 */
export function BlockBuilder({
  data = { name: '', description: '', questions: [] },
  onChange = () => {},
  onDropQuestionFromSidebar,
  isModal = false,
  isOpen = true,
  onClose,
  onSave,
  modalTitle = 'Configurar bloque',
}) {
  const [activeMenuIndex, setActiveMenuIndex] = useState(null);
  const [questionModalMode, setQuestionModalMode] = useState(null);
  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState(null);
  const [editingQuestionData, setEditingQuestionData] = useState(INITIAL_QUESTION_STATE);
  const [dropIndex, setDropIndex] = useState(null);
  const [reorderMessage, setReorderMessage] = useState('');

  const questions = data.questions || [];
  const selectedQuestion =
    selectedQuestionIndex === null ? null : questions[selectedQuestionIndex];

  function handleField(field, value) {
    onChange({ ...data, [field]: value });
  }

  function getDragData(event) {
    try {
      const value = event.dataTransfer.getData('application/x-formquid-catalog-item') || event.dataTransfer.getData('text/plain');
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  }

  function preventDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = 'none';
  }

  function handleDragStart(event, index) {
    event.dataTransfer.setData(
      'text/plain',
      JSON.stringify({ type: 'INTERNAL_SORT', index }),
    );
    event.dataTransfer.effectAllowed = 'move';
  }

  function moveQuestion(fromIndex, toIndex) {
    if (fromIndex < 0 || fromIndex >= questions.length) return;

    const updated = [...questions];
    const [question] = updated.splice(fromIndex, 1);
    const adjustedIndex = fromIndex < toIndex ? toIndex - 1 : toIndex;
    const insertionIndex = Math.max(0, Math.min(adjustedIndex, updated.length));

    if (insertionIndex === fromIndex) return;
    updated.splice(insertionIndex, 0, question);

    handleField('questions', updated);
    setReorderMessage(`Pregunta movida a la posición ${insertionIndex + 1}.`);
  }

  function getTargetIndex(event, questionIndex) {
    const bounds = event.currentTarget.getBoundingClientRect();
    return event.clientY < bounds.top + bounds.height / 2 ? questionIndex : questionIndex + 1;
  }

  function handleQuestionDragOver(event, questionIndex) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'move' ? 'move' : 'copy';
    setDropIndex(getTargetIndex(event, questionIndex));
  }

  function handleQuestionDrop(event, questionIndex) {
    const dragData = getDragData(event);
    event.preventDefault();
    event.stopPropagation();
    setDropIndex(null);
    const targetIndex = getTargetIndex(event, questionIndex);

    if (dragData?.type === 'CATALOG_QUESTION') {
      onDropQuestionFromSidebar?.(dragData.question, targetIndex);
    } else if (dragData?.type === 'INTERNAL_SORT') {
      moveQuestion(dragData.index, targetIndex);
    }
  }

  function handleStructureDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    setDropIndex(null);
    const dragData = getDragData(event);

    if (dragData?.type === 'CATALOG_QUESTION') {
      onDropQuestionFromSidebar?.(dragData.question, questions.length);
      return;
    }

    if (dragData?.type === 'INTERNAL_SORT') {
      moveQuestion(dragData.index, questions.length);
    }
  }

  function handleReorderKeyDown(event, index) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    moveQuestion(index, event.key === 'ArrowUp' ? index - 1 : index + 2);
  }

  function openCreateQuestionModal() {
    setEditingQuestionData({
      ...INITIAL_QUESTION_STATE,
      isRequired: false,
    });
    setSelectedQuestionIndex(null);
    setQuestionModalMode('CREATE');
  }

  function openQuestionModal(mode, index) {
    setSelectedQuestionIndex(index);
    setActiveMenuIndex(null);

    if (mode === 'EDIT') {
      setEditingQuestionData({ ...questions[index] });
    }

    setQuestionModalMode(mode);
  }

  function closeQuestionModal() {
    setQuestionModalMode(null);
    setSelectedQuestionIndex(null);
    setEditingQuestionData(INITIAL_QUESTION_STATE);
  }

  function handleSaveQuestion() {
    if (questionModalMode === 'CREATE') {
      handleField('questions', [
        ...questions,
        {
          ...editingQuestionData,
          isCatalog: false,
        },
      ]);

      closeQuestionModal();
      return;
    }

    if (questionModalMode === 'EDIT' && selectedQuestionIndex !== null) {
      const updated = [...questions];
      updated[selectedQuestionIndex] = { ...editingQuestionData };

      handleField('questions', updated);
      closeQuestionModal();
    }
  }

  function handleDeleteQuestion() {
    if (selectedQuestionIndex === null) return;

    handleField(
      'questions',
      questions.filter((_, index) => index !== selectedQuestionIndex),
    );

    closeQuestionModal();
  }

  const builderContent = (
    <div className={`block-builder-stack ${isModal ? 'modal-mode' : ''}`}>
      <section
        className="builder-card"
        onDragOverCapture={preventDrop}
        onDropCapture={preventDrop}
      >
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
            value={data.name || ''}
            onChange={(event) => handleField('name', event.target.value)}
          />

          <span className="field-counter">
            {(data.name || '').length}/150
          </span>
        </div>

        <div className="builder-field">
          <label htmlFor="block-description">
            Descripción
          </label>

          <textarea
            id="block-description"
            rows={4}
            placeholder="Añade detalles o contexto sobre el propósito de este bloque de preguntas..."
            maxLength={500}
            value={data.description || ''}
            onChange={(event) => handleField('description', event.target.value)}
          />

          <span className="field-counter">
            {(data.description || '').length}/500
          </span>
        </div>
      </section>

      <section
        className="builder-card block-structure-card"
        onDragOver={(event) => event.preventDefault()}
        onDrop={handleStructureDrop}
        onDragLeave={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget)) setDropIndex(null);
        }}
      >
        <div className="structure-header">
          <h2 className="builder-section-title">
            ESTRUCTURA DEL BLOQUE
          </h2>

          <button
            type="button"
            className="btn-add-block-question"
            onClick={openCreateQuestionModal}
          >
            <MdAdd aria-hidden="true" />
            <span>Añadir nueva pregunta</span>
          </button>
        </div>

        <div className="block-questions-list">
          <p className="sr-only" aria-live="polite">{reorderMessage}</p>

          {questions.map((question, index) => {
            const isMenuOpen = activeMenuIndex === index;
            const typeConfig = QUESTION_TYPES.find(
              (type) => type.id === question.questionType,
            );
            const TypeIcon = typeConfig?.icon;

            return (
              <div
                key={question.id || index}
                className={`block-question-item ${dropIndex === index ? 'drop-before' : ''} ${index === questions.length - 1 && dropIndex === questions.length ? 'drop-after' : ''}`}
                onDragOver={(event) => handleQuestionDragOver(event, index)}
                onDrop={(event) => handleQuestionDrop(event, index)}
              >
                <div className="question-item-left">
                  <button
                    type="button"
                    className="drag-handle"
                    draggable
                    aria-label={`Reordenar pregunta ${index + 1}. Usa las flechas arriba y abajo o arrastra el control.`}
                    title="Arrastra o usa las flechas para reordenar"
                    onDragStart={(event) => handleDragStart(event, index)}
                    onKeyDown={(event) => handleReorderKeyDown(event, index)}
                  >
                    <MdDragIndicator aria-hidden="true" />
                  </button>

                  {TypeIcon && (
                    <span
                      className="question-type-badge"
                      style={{
                        color: typeConfig.color,
                        borderColor: `${typeConfig.color}60`,
                      }}
                      title={typeConfig.title}
                    >
                      <TypeIcon aria-hidden="true" />
                    </span>
                  )}
                </div>

                <div className="question-item-preview">
                  <Question
                    questionNumber={`${index + 1}`}
                    data={question}
                    interactive={false}
                  />
                </div>

                <div className="question-item-actions">
                  <button
                    type="button"
                    className="action-dots-btn"
                    aria-label="Opciones de la pregunta"
                    onClick={() =>
                      setActiveMenuIndex(isMenuOpen ? null : index)
                    }
                  >
                    <MdMoreVert aria-hidden="true" />
                  </button>

                  {isMenuOpen && (
                    <div className="actions-floating-menu">
                      <button
                        type="button"
                        onClick={() => openQuestionModal('VIEW', index)}
                      >
                        <MdOutlineVisibility aria-hidden="true" />
                        Ver detalles
                      </button>

                      <button
                        type="button"
                        onClick={() => openQuestionModal('EDIT', index)}
                      >
                        <MdEdit aria-hidden="true" />
                        Editar pregunta
                      </button>

                      <button
                        type="button"
                        className="delete-opt"
                        onClick={() => openQuestionModal('DELETE', index)}
                      >
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
          className={`block-empty-dropzone ${dropIndex === questions.length ? 'drop-active' : ''}`}
          onDragOver={(event) => {
            event.preventDefault();
            event.stopPropagation();
            event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'move' ? 'move' : 'copy';
            setDropIndex(questions.length);
          }}
          onDrop={handleStructureDrop}
        >
          <div className="dropzone-text-group">
            <span className="dropzone-title">
              <MdAdd aria-hidden="true" />
              Agrega preguntas
            </span>

            <p className="dropzone-sub">
              Arrástralas desde el catálogo o créalas con el botón superior.
            </p>
          </div>
        </div>
      </section>

      {(questionModalMode === 'CREATE' ||
        questionModalMode === 'EDIT') && (
        <QuestionBuilder
          data={editingQuestionData}
          onChange={setEditingQuestionData}
          isCatalog={false}
          isModal
          isOpen
          modalTitle={
            questionModalMode === 'CREATE'
              ? 'CREAR PREGUNTA PROPIA'
              : 'EDITAR PREGUNTA'
          }
          onClose={closeQuestionModal}
          onSave={handleSaveQuestion}
        />
      )}

      {questionModalMode === 'VIEW' && selectedQuestion && (
        <div
          className="block-modal-overlay"
          onClick={closeQuestionModal}
        >
          <div
            className="block-modal-container"
            role="dialog"
            aria-modal="true"
            aria-labelledby="question-details-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="block-modal-header">
              <h2 id="question-details-title">
                DETALLE DE LA PREGUNTA
              </h2>

              <button
                type="button"
                className="btn-close-modal"
                aria-label="Cerrar modal"
                onClick={closeQuestionModal}
              >
                <MdClose aria-hidden="true" />
              </button>
            </header>

            <div className="block-modal-body">
              <Question
                questionNumber={`${selectedQuestionIndex + 1}`}
                data={selectedQuestion}
                interactive={false}
              />
            </div>
          </div>
        </div>
      )}

      {questionModalMode === 'DELETE' && selectedQuestion && (
        <div
          className="block-modal-overlay"
          onClick={closeQuestionModal}
        >
          <div
            className="block-modal-container delete-modal-box"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-question-title"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="block-modal-header">
              <h2 id="delete-question-title">
                ELIMINAR PREGUNTA
              </h2>

              <button
                type="button"
                className="btn-close-modal"
                aria-label="Cerrar modal"
                onClick={closeQuestionModal}
              >
                <MdClose aria-hidden="true" />
              </button>
            </header>

            <div className="block-modal-body">
              <p>
                ¿Estás seguro de que deseas retirar esta pregunta del bloque?
              </p>
            </div>

            <footer className="block-modal-footer">
              <button
                type="button"
                className="btn-cancel"
                onClick={closeQuestionModal}
              >
                Cancelar
              </button>

              <button
                type="button"
                className="btn-danger-confirm"
                onClick={handleDeleteQuestion}
              >
                Eliminar
              </button>
            </footer>
          </div>
        </div>
      )}
    </div>
  );

  if (!isModal) {
    return builderContent;
  }

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="block-modal-overlay"
      onClick={onClose}
    >
      <div
        className="block-modal-container block-builder-modal-wrapper"
        role="dialog"
        aria-modal="true"
        aria-labelledby="block-builder-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="block-modal-header">
          <h2 id="block-builder-modal-title">
            {modalTitle}
          </h2>

          {onClose && (
            <button
              type="button"
              className="btn-close-modal"
              aria-label="Cerrar modal"
              onClick={onClose}
            >
              <MdClose aria-hidden="true" />
            </button>
          )}
        </header>

        <div className="block-modal-body">
          {builderContent}
        </div>

        {onSave && (
          <footer className="block-modal-footer">
            <button type="button" className="btn-confirm" onClick={onSave}>Guardar bloque</button>
          </footer>
        )}
      </div>
    </div>
  );
}