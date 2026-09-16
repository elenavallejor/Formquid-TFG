import { useState } from 'react';
import {
  MdAdd,
  MdClose,
  MdDeleteOutline,
  MdDragIndicator,
  MdEdit,
  MdLayers,
  MdMoreVert,
} from 'react-icons/md';

import {
  INITIAL_BLOCK_STATE,
  buildBlockTextItem,
  getBlockBuilderValidationError,
} from '../../utils/blockBuilderUtils.js';
import { getDragData, getDropIndex, reorderItems } from '../../utils/builderDragUtils.js';
import { INITIAL_QUESTION_STATE, QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import {
  INITIAL_TEMPLATE_STATE,
  addCatalogQuestionToTemplateBlock,
  buildEditableTemplateBlock,
} from '../../utils/templateBuilderUtils.js';
import { Block } from '../Block/Block.jsx';
import { BlockBuilder } from '../BlockBuilder/BlockBuilder.jsx';
import {
  CREATABLE_ELEMENT_TYPE,
  CreateElementButton,
  ELEMENT_CREATOR_CONTEXT,
} from '../CreateElementButton/CreateElementButton.jsx';
import { ErrorMessage } from '../ErrorMessage/ErrorMessage.jsx';
import { Question } from '../Question/Question.jsx';
import { QuestionBuilder } from '../QuestionBuilder/QuestionBuilder.jsx';
import { RequiredToggle } from '../RequiredToggle/RequiredToggle.jsx';
import { TextContent, TextContentIcon } from '../TextContent/TextContent.jsx';

import '../BlockBuilder/BlockBuilder.css';
import './TemplateBuilder.css';

function createLocalId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function getItemLabel(item) {
  if (item.itemType === 'TEXT') return 'texto';
  if (item.itemType === 'BLOCK') return 'bloque';
  return 'pregunta';
}

/**
 * Gestiona la estructura ordenada de una plantilla y sus editores locales.
 */
export function TemplateBuilder({
  data = INITIAL_TEMPLATE_STATE,
  onChange = () => {},
  onDropQuestionFromSidebar,
  onDropBlockFromSidebar,
}) {
  const [activeMenuIndex, setActiveMenuIndex] = useState(null);
  const [modalMode, setModalMode] = useState(null);
  const [selectedItemIndex, setSelectedItemIndex] = useState(null);
  const [selectedBlockQuestion, setSelectedBlockQuestion] = useState(null);
  const [editingQuestion, setEditingQuestion] = useState(INITIAL_QUESTION_STATE);
  const [editingText, setEditingText] = useState('');
  const [editingBlock, setEditingBlock] = useState(INITIAL_BLOCK_STATE);
  const [modalError, setModalError] = useState('');
  const [dropIndex, setDropIndex] = useState(null);
  const [reorderMessage, setReorderMessage] = useState('');

  const items = data.items || [];
  const selectedItem = selectedItemIndex === null ? null : items[selectedItemIndex];
  let questionCount = 0;
  const questionNumberByIndex = items.map((item) =>
    item.itemType === 'QUESTION' ? ++questionCount : null,
  );

  function updateData(changes) {
    onChange({ ...data, ...changes });
  }

  function updateItems(nextItems) {
    updateData({ items: nextItems });
  }

  function preventDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = 'none';
  }

  function moveItem(fromIndex, toIndex) {
    const updated = reorderItems(items, fromIndex, toIndex);
    if (updated === items) return;
    updateItems(updated);
    setReorderMessage('Orden de los elementos actualizado.');
  }

  function handleDropAt(event, targetIndex) {
    event.preventDefault();
    event.stopPropagation();
    setDropIndex(null);

    const dragData = getDragData(event);
    if (dragData?.type === 'CATALOG_QUESTION') {
      onDropQuestionFromSidebar?.(dragData.question, targetIndex);
    } else if (dragData?.type === 'CATALOG_BLOCK') {
      onDropBlockFromSidebar?.(dragData.block, targetIndex);
    } else if (dragData?.type === 'INTERNAL_SORT') {
      moveItem(dragData.index, targetIndex);
    } else if (dragData?.type === 'BLOCK_INTERNAL_SORT') {
      handleMoveQuestionOutOfBlock(
        dragData.blockIndex,
        dragData.index,
        targetIndex,
      );
    }
  }

  function handleItemDragOver(event, itemIndex) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'move' ? 'move' : 'copy';
    setDropIndex(getDropIndex(event, itemIndex));
  }

  function handleReorderKeyDown(event, index) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    moveItem(index, event.key === 'ArrowUp' ? index - 1 : index + 2);
  }

  function handleRequiredChange(index, isRequired) {
    const updated = [...items];
    updated[index] = { ...updated[index], isRequired };
    updateItems(updated);
  }

  function handleDropQuestionIntoBlock(blockIndex, question, targetIndex) {
    const updated = [...items];
    updated[blockIndex] = addCatalogQuestionToTemplateBlock(
      updated[blockIndex],
      question,
      targetIndex,
    );
    updateItems(updated);
  }

  function buildLocalBlockItem(templateBlock, transformItems) {
    const block = buildEditableTemplateBlock(templateBlock.block || templateBlock);
    const nextItems = transformItems(block.items);
    if (nextItems === block.items) return templateBlock;

    const localBlock = { ...templateBlock };
    delete localBlock.blockId;
    delete localBlock.questionCount;
    return {
      ...localBlock,
      block: { ...block, items: nextItems },
    };
  }

  function buildLocalQuestion(question) {
    const localQuestion = {
      ...question,
      itemType: 'QUESTION',
      localId: question.localId || createLocalId('question'),
    };
    delete localQuestion.id;
    delete localQuestion.fixedQuestionId;
    return localQuestion;
  }

  function updateBlockContent(blockIndex, transformItems) {
    const updated = [...items];
    const nextBlock = buildLocalBlockItem(updated[blockIndex], transformItems);
    if (nextBlock === updated[blockIndex]) return;
    updated[blockIndex] = nextBlock;
    updateItems(updated);
  }

  function handleMoveItemInsideBlock(blockIndex, fromIndex, toIndex) {
    updateBlockContent(
      blockIndex,
      (blockItems) => reorderItems(blockItems, fromIndex, toIndex),
    );
  }

  function handleDeleteItemInsideBlock(blockIndex, itemIndex) {
    updateBlockContent(
      blockIndex,
      (blockItems) => blockItems.filter((_, index) => index !== itemIndex),
    );
  }

  function handleRequiredChangeInsideBlock(blockIndex, itemIndex, isRequired) {
    updateBlockContent(blockIndex, (blockItems) => {
      const updated = [...blockItems];
      updated[itemIndex] = { ...updated[itemIndex], isRequired };
      return updated;
    });
  }

  function handleMoveTopLevelQuestionIntoBlock(sourceIndex, blockIndex, targetIndex) {
    const question = items[sourceIndex];
    if (question?.itemType !== 'QUESTION') return;

    const updated = [...items];
    updated[blockIndex] = buildLocalBlockItem(updated[blockIndex], (blockItems) => {
      const nextItems = [...blockItems];
      const insertionIndex = Math.max(0, Math.min(targetIndex, nextItems.length));
      nextItems.splice(insertionIndex, 0, buildLocalQuestion(question));
      return nextItems;
    });
    updated.splice(sourceIndex, 1);
    updateItems(updated);
  }

  function handleMoveQuestionOutOfBlock(blockIndex, itemIndex, targetIndex) {
    const block = buildEditableTemplateBlock(items[blockIndex]?.block || items[blockIndex]);
    const question = block.items[itemIndex];
    if (!question || question.itemType === 'TEXT') return;

    const updated = [...items];
    updated[blockIndex] = buildLocalBlockItem(
      updated[blockIndex],
      (blockItems) => blockItems.filter((_, index) => index !== itemIndex),
    );
    const insertionIndex = Math.max(0, Math.min(targetIndex, updated.length));
    updated.splice(insertionIndex, 0, buildLocalQuestion(question));
    updateItems(updated);
  }

  function handleMoveQuestionBetweenBlocks(
    sourceBlockIndex,
    sourceItemIndex,
    targetBlockIndex,
    targetIndex,
  ) {
    if (sourceBlockIndex === targetBlockIndex) {
      handleMoveItemInsideBlock(sourceBlockIndex, sourceItemIndex, targetIndex);
      return;
    }

    const sourceBlock = buildEditableTemplateBlock(
      items[sourceBlockIndex]?.block || items[sourceBlockIndex],
    );
    const question = sourceBlock.items[sourceItemIndex];
    if (!question || question.itemType === 'TEXT') return;

    const updated = [...items];
    updated[sourceBlockIndex] = buildLocalBlockItem(
      updated[sourceBlockIndex],
      (blockItems) => blockItems.filter((_, index) => index !== sourceItemIndex),
    );
    updated[targetBlockIndex] = buildLocalBlockItem(
      updated[targetBlockIndex],
      (blockItems) => {
        const nextItems = [...blockItems];
        const insertionIndex = Math.max(0, Math.min(targetIndex, nextItems.length));
        nextItems.splice(insertionIndex, 0, buildLocalQuestion(question));
        return nextItems;
      },
    );
    updateItems(updated);
  }

  function openBlockQuestionModal(blockIndex, itemIndex) {
    const block = buildEditableTemplateBlock(items[blockIndex]?.block || items[blockIndex]);
    const question = block.items[itemIndex];
    if (!question || question.itemType === 'TEXT') return;

    setEditingQuestion({ ...question });
    setSelectedItemIndex(null);
    setSelectedBlockQuestion({ blockIndex, itemIndex });
    setModalError('');
    setModalMode('EDIT_BLOCK_QUESTION');
  }

  function openCreateQuestionModal() {
    setEditingQuestion({ ...INITIAL_QUESTION_STATE, itemType: 'QUESTION', isRequired: false });
    setSelectedItemIndex(null);
    setSelectedBlockQuestion(null);
    setModalError('');
    setModalMode('CREATE_QUESTION');
  }

  function openCreateTextModal() {
    setEditingText('');
    setSelectedItemIndex(null);
    setSelectedBlockQuestion(null);
    setModalError('');
    setModalMode('CREATE_TEXT');
  }

  function openCreateBlockModal() {
    setEditingBlock({ ...INITIAL_BLOCK_STATE, items: [] });
    setSelectedItemIndex(null);
    setSelectedBlockQuestion(null);
    setModalError('');
    setModalMode('CREATE_BLOCK');
  }

  function handleCreateElement(type) {
    if (type === CREATABLE_ELEMENT_TYPE.QUESTION) openCreateQuestionModal();
    if (type === CREATABLE_ELEMENT_TYPE.TEXT) openCreateTextModal();
    if (type === CREATABLE_ELEMENT_TYPE.BLOCK) openCreateBlockModal();
  }

  function openItemModal(mode, index) {
    const item = items[index];
    setSelectedItemIndex(index);
    setSelectedBlockQuestion(null);
    setActiveMenuIndex(null);
    setModalError('');
    if (mode === 'EDIT_QUESTION') setEditingQuestion({ ...item });
    if (mode === 'EDIT_TEXT') setEditingText(item.textBody || '');
    if (mode === 'EDIT_BLOCK') setEditingBlock(buildEditableTemplateBlock(item.block));
    setModalMode(mode);
  }

  function closeModal() {
    setModalMode(null);
    setSelectedItemIndex(null);
    setSelectedBlockQuestion(null);
    setEditingQuestion(INITIAL_QUESTION_STATE);
    setEditingText('');
    setEditingBlock(INITIAL_BLOCK_STATE);
    setModalError('');
  }

  function handleSaveQuestion() {
    const question = { ...editingQuestion, itemType: 'QUESTION' };
    delete question.fixedQuestionId;

    if (modalMode === 'EDIT_BLOCK_QUESTION' && selectedBlockQuestion) {
      updateBlockContent(selectedBlockQuestion.blockIndex, (blockItems) => {
        const updated = [...blockItems];
        updated[selectedBlockQuestion.itemIndex] = buildLocalQuestion(question);
        return updated;
      });
    } else if (modalMode === 'CREATE_QUESTION') {
      updateItems([...items, { ...question, localId: createLocalId('question') }]);
    } else if (selectedItemIndex !== null) {
      const updated = [...items];
      updated[selectedItemIndex] = question;
      updateItems(updated);
    }
    closeModal();
  }

  function handleSaveText() {
    const normalizedText = editingText.trim();
    if (!normalizedText) {
      setModalError('El texto no puede estar vacío.');
      return;
    }

    if (modalMode === 'CREATE_TEXT') {
      updateItems([...items, buildBlockTextItem(normalizedText)]);
    } else if (selectedItemIndex !== null) {
      const updated = [...items];
      updated[selectedItemIndex] = { ...updated[selectedItemIndex], textBody: normalizedText };
      updateItems(updated);
    }
    closeModal();
  }

  function handleSaveBlock() {
    const validationError = getBlockBuilderValidationError(editingBlock);
    if (validationError) {
      setModalError(validationError);
      return;
    }

    const blockItem = {
      itemType: 'BLOCK',
      localId: selectedItem?.localId || createLocalId('block'),
      block: editingBlock,
    };
    if (modalMode === 'CREATE_BLOCK') {
      updateItems([...items, blockItem]);
    } else if (selectedItemIndex !== null) {
      const updated = [...items];
      updated[selectedItemIndex] = blockItem;
      updateItems(updated);
    }
    closeModal();
  }

  function handleDeleteItem() {
    if (selectedItemIndex === null) return;
    updateItems(items.filter((_, index) => index !== selectedItemIndex));
    closeModal();
  }

  return (
    <div className="block-builder-stack template-builder-stack">
      <section className="builder-card" onDragOverCapture={preventDrop} onDropCapture={preventDrop}>
        <h2 className="builder-section-title">INFORMACIÓN GENERAL</h2>
        <div className="builder-field">
          <label htmlFor="template-title">Título de la plantilla <span>*</span></label>
          <input id="template-title" type="text" placeholder="Introduce un título para la plantilla..." maxLength={200} value={data.title || ''} onChange={(event) => updateData({ title: event.target.value })} />
          <span className="field-counter">{(data.title || '').length}/200</span>
        </div>
        <div className="builder-field">
          <label htmlFor="template-description">Descripción</label>
          <textarea id="template-description" rows={4} placeholder="Añade detalles o contexto sobre el propósito de esta plantilla..." maxLength={500} value={data.description || ''} onChange={(event) => updateData({ description: event.target.value })} />
          <span className="field-counter">{(data.description || '').length}/500</span>
        </div>
      </section>

      <section className="builder-card block-structure-card" onDragOver={(event) => event.preventDefault()} onDrop={(event) => handleDropAt(event, items.length)} onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setDropIndex(null); }}>
        <div className="structure-header">
          <div>
            <h2 className="builder-section-title">ESTRUCTURA DE LA PLANTILLA</h2>
            <p className="builder-section-desc">Combina preguntas, textos y bloques, y ordénalos como aparecerán.</p>
          </div>
          <CreateElementButton context={ELEMENT_CREATOR_CONTEXT.TEMPLATE} onSelect={handleCreateElement} />
        </div>

        <div className="block-questions-list">
          <p className="sr-only" aria-live="polite">{reorderMessage}</p>
          {items.map((item, index) => {
            const isText = item.itemType === 'TEXT';
            const isBlock = item.itemType === 'BLOCK';
            const isMenuOpen = activeMenuIndex === index;
            const typeConfig = !isText && !isBlock ? QUESTION_TYPES.find((type) => type.id === item.questionType) : null;
            const TypeIcon = typeConfig?.icon;
            const block = item.block || item;

            return (
              <div key={item.localId || item.fixedQuestionId || item.blockId || index} className={`block-question-item ${dropIndex === index ? 'drop-before' : ''} ${index === items.length - 1 && dropIndex === items.length ? 'drop-after' : ''}`} onDragOver={(event) => handleItemDragOver(event, index)} onDrop={(event) => handleDropAt(event, getDropIndex(event, index))}>
                <div className="question-item-left">
                  <button type="button" className="drag-handle" draggable aria-label={`Reordenar elemento ${index + 1}. Usa las flechas o arrastra.`} onDragStart={(event) => { if (!isText && !isBlock) event.dataTransfer.setData('application/x-formquid-template-question', '1'); event.dataTransfer.setData('text/plain', JSON.stringify({ type: 'INTERNAL_SORT', index })); event.dataTransfer.effectAllowed = 'move'; }} onDragEnd={() => setDropIndex(null)} onKeyDown={(event) => handleReorderKeyDown(event, index)}><MdDragIndicator aria-hidden="true" /></button>
                  {isText ? <TextContentIcon /> : isBlock ? (
                    <span className="question-type-badge template-block-badge" title="Bloque de preguntas"><MdLayers aria-hidden="true" /></span>
                  ) : TypeIcon ? (
                    <span className="question-type-badge" style={{ color: typeConfig.color, borderColor: `${typeConfig.color}60` }} title={typeConfig.title}><TypeIcon aria-hidden="true" /></span>
                  ) : null}
                </div>

                <div className="question-item-preview">
                  {isText ? <TextContent data={item} /> : isBlock ? (
                    <Block
                      data={block}
                      onDropQuestion={(question, targetIndex) =>
                        handleDropQuestionIntoBlock(index, question, targetIndex)}
                      onMoveItem={(fromIndex, targetIndex) =>
                        handleMoveItemInsideBlock(index, fromIndex, targetIndex)}
                      onDeleteItem={(itemIndex) =>
                        handleDeleteItemInsideBlock(index, itemIndex)}
                      blockIndex={index}
                      onDropTemplateQuestion={(sourceIndex, targetIndex) =>
                        handleMoveTopLevelQuestionIntoBlock(sourceIndex, index, targetIndex)}
                      onDropBlockQuestion={(sourceBlockIndex, sourceItemIndex, targetIndex) =>
                        handleMoveQuestionBetweenBlocks(
                          sourceBlockIndex,
                          sourceItemIndex,
                          index,
                          targetIndex,
                        )}
                      onEditItem={(itemIndex) => openBlockQuestionModal(index, itemIndex)}
                      onRequiredChange={(itemIndex, isRequired) =>
                        handleRequiredChangeInsideBlock(index, itemIndex, isRequired)}
                      onDragInside={() => setDropIndex(null)}
                    />
                  ) : <Question questionNumber={`${questionNumberByIndex[index]}`} data={item} interactive={false} />}
                </div>

                <div className="question-item-actions">
                  {!isText && !isBlock && (
                    <RequiredToggle
                      checked={item.isRequired}
                      onChange={(isRequired) => handleRequiredChange(index, isRequired)}
                    />
                  )}
                  <button type="button" className="action-dots-btn" aria-label={`Opciones de ${getItemLabel(item)}`} onClick={() => setActiveMenuIndex(isMenuOpen ? null : index)}><MdMoreVert aria-hidden="true" /></button>
                  {isMenuOpen && (
                    <div className="actions-floating-menu">
                      {!isBlock && <button type="button" onClick={() => openItemModal(isText ? 'EDIT_TEXT' : 'EDIT_QUESTION', index)}><MdEdit aria-hidden="true" /> Editar {getItemLabel(item)}</button>}
                      {isBlock && <button type="button" onClick={() => openItemModal('EDIT_BLOCK', index)}><MdEdit aria-hidden="true" /> Editar bloque</button>}
                      <button type="button" className="delete-opt" onClick={() => openItemModal('DELETE', index)}><MdDeleteOutline aria-hidden="true" /> Eliminar de la plantilla</button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div className={`block-empty-dropzone ${dropIndex === items.length ? 'drop-active' : ''}`} onDragOver={(event) => { event.preventDefault(); event.stopPropagation(); event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'move' ? 'move' : 'copy'; setDropIndex(items.length); }} onDrop={(event) => handleDropAt(event, items.length)}>
          <div className="dropzone-text-group">
            <span className="dropzone-title"><MdAdd aria-hidden="true" /> Agrega contenido</span>
            <p className="dropzone-sub">Arrastra elementos desde el catálogo o créalos con el botón superior.</p>
          </div>
        </div>
      </section>

      {(modalMode === 'CREATE_QUESTION' || modalMode === 'EDIT_QUESTION' || modalMode === 'EDIT_BLOCK_QUESTION') && <QuestionBuilder data={editingQuestion} onChange={setEditingQuestion} isCatalog={false} isModal modalTitle={modalMode === 'CREATE_QUESTION' ? 'Crear pregunta propia' : 'Editar pregunta'} onClose={closeModal} onSave={handleSaveQuestion} />}

      {(modalMode === 'CREATE_TEXT' || modalMode === 'EDIT_TEXT') && (
        <div className="block-modal-overlay" onClick={closeModal}>
          <section className="block-modal-container text-editor-modal" role="dialog" aria-modal="true" aria-labelledby="template-text-title" onClick={(event) => event.stopPropagation()}>
            <header className="block-modal-header"><h2 id="template-text-title">{modalMode === 'CREATE_TEXT' ? 'AÑADIR TEXTO' : 'EDITAR TEXTO'}</h2><button type="button" className="btn-close-modal" aria-label="Cerrar modal" onClick={closeModal}><MdClose aria-hidden="true" /></button></header>
            <div className="block-modal-body">
              <div className="builder-field"><label htmlFor="template-text-body">Contenido <span>*</span></label><textarea id="template-text-body" rows={8} maxLength={5000} value={editingText} onChange={(event) => { setEditingText(event.target.value); setModalError(''); }} autoFocus /><span className="field-counter">{editingText.length}/5000</span></div>
              <ErrorMessage message={modalError} onClose={() => setModalError('')} />
            </div>
            <footer className="block-modal-footer"><button type="button" className="btn-cancel" onClick={closeModal}>Cancelar</button><button type="button" className="btn-confirm" onClick={handleSaveText}>Guardar texto</button></footer>
          </section>
        </div>
      )}

      {(modalMode === 'CREATE_BLOCK' || modalMode === 'EDIT_BLOCK') && (
        <BlockBuilder
          data={editingBlock}
          onChange={(nextBlock) => { setEditingBlock(nextBlock); setModalError(''); }}
          isModal
          modalTitle={modalMode === 'CREATE_BLOCK' ? 'CREAR BLOQUE' : 'EDITAR BLOQUE'}
          errorMessage={modalError}
          onClearError={() => setModalError('')}
          onClose={closeModal}
          onSave={handleSaveBlock}
        />
      )}

      {modalMode === 'DELETE' && selectedItem && (
        <div className="block-modal-overlay" onClick={closeModal}><section className="block-modal-container delete-modal-box" role="dialog" aria-modal="true" aria-labelledby="template-delete-title" onClick={(event) => event.stopPropagation()}><header className="block-modal-header"><h2 id="template-delete-title">ELIMINAR {getItemLabel(selectedItem).toUpperCase()}</h2><button type="button" className="btn-close-modal" aria-label="Cerrar modal" onClick={closeModal}><MdClose aria-hidden="true" /></button></header><div className="block-modal-body"><p>¿Estás seguro de que deseas retirar este elemento de la plantilla?</p></div><footer className="block-modal-footer"><button type="button" className="btn-cancel" onClick={closeModal}>Cancelar</button><button type="button" className="btn-danger-confirm" onClick={handleDeleteItem}>Eliminar</button></footer></section></div>
      )}
    </div>
  );
}
