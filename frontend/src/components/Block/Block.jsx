import { useState } from 'react';
import { MdAdd, MdDeleteOutline, MdDragIndicator, MdEdit, MdKeyboardArrowDown, MdKeyboardArrowRight, MdKeyboardArrowUp, MdMoreVert } from 'react-icons/md';

import { buildBlockQuestionFromCatalog, getBlockItems } from '../../utils/blockBuilderUtils.js';
import { getDragData, getDropIndex } from '../../utils/builderDragUtils.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { Question } from '../Question/Question.jsx';
import { RequiredToggle } from '../RequiredToggle/RequiredToggle.jsx';
import { TextContent, TextContentIcon } from '../TextContent/TextContent.jsx';

import './Block.css';

function getDisplayItems(data) {
  if (Array.isArray(data.contentItems)) return data.contentItems;
  return getBlockItems(data);
}

function getQuestionData(item) {
  if (!item.question) return item;
  return {
    ...buildBlockQuestionFromCatalog(item.question),
    itemType: 'QUESTION',
    isRequired: Boolean(item.required),
  };
}

/**
 * Representa el contenido completo de un bloque.
 * Puede habilitar puntos de inserción para preguntas arrastradas del catálogo.
 */
export function Block({
  data = {},
  blockIndex,
  blockLabel,
  headingLevel = 3,
  questionNumberStart = 0,
  onDropQuestion,
  onDropTemplateQuestion,
  onDropBlockItem,
  onMoveItem,
  onEditItem,
  onDeleteItem,
  onRequiredChange,
  moveTargets = [],
  onMoveItemOut,
  onMoveItemToTarget,
  onDragInside,
}) {
  const normalizedHeadingLevel = Math.min(6, Math.max(2, Number(headingLevel) || 3));
  const Heading = `h${normalizedHeadingLevel}`;
  const [dropIndex, setDropIndex] = useState(null);
  const [activeMenuIndex, setActiveMenuIndex] = useState(null);
  const items = getDisplayItems(data);
  let questionNumber = 0;

  function handleDragOver(event, itemIndex) {
    const dragTypes = Array.from(event.dataTransfer.types);
    const isCatalogQuestion = onDropQuestion
      && dragTypes.includes('application/x-formquid-question');
    const isBlockItem = onMoveItem
      && dragTypes.includes('application/x-formquid-block-item');
    const isTemplateQuestion = onDropTemplateQuestion
      && dragTypes.includes('application/x-formquid-template-question');
    if (!isCatalogQuestion && !isBlockItem && !isTemplateQuestion) return;
    event.preventDefault();
    event.stopPropagation();
    onDragInside?.();
    event.dataTransfer.dropEffect = isCatalogQuestion ? 'copy' : 'move';
    setDropIndex(getDropIndex(event, itemIndex));
  }

  function handleDrop(event, targetIndex) {
    const dragData = getDragData(event);
    setDropIndex(null);
    if (dragData?.type === 'CATALOG_QUESTION' && onDropQuestion) {
      event.preventDefault();
      event.stopPropagation();
      onDropQuestion(dragData.question, targetIndex);
      return;
    }
    if (dragData?.type === 'INTERNAL_SORT' && onDropTemplateQuestion) {
      event.preventDefault();
      event.stopPropagation();
      onDropTemplateQuestion(dragData.index, targetIndex);
      return;
    }
    if (dragData?.type !== 'BLOCK_INTERNAL_SORT') return;

    event.preventDefault();
    event.stopPropagation();
    if (dragData.blockIndex === blockIndex) {
      onMoveItem?.(dragData.index, targetIndex);
    } else {
      onDropBlockItem?.(dragData.blockIndex, dragData.index, targetIndex);
    }
  }

  return (
    <article
      className="block-content"
      onDragLeave={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setDropIndex(null);
      }}
    >
      <header className="block-content-header">
        <Heading className="block-content-title">
          {blockLabel && <span className="block-prefix">{blockLabel}.</span>}{' '}
          {data.name || 'Bloque sin nombre'}
        </Heading>
        {data.description && <p>{data.description}</p>}
      </header>

      <div className="block-content-items">
        {items.map((item, index) => {
          const isText = item.itemType === 'TEXT';
          const currentQuestionNumber = isText ? null : questionNumberStart + ++questionNumber;
          const questionData = isText ? null : getQuestionData(item);
          const typeConfig = isText
            ? null
            : QUESTION_TYPES.find((type) => type.id === questionData.questionType);
          const TypeIcon = typeConfig?.icon;
          const isMenuOpen = activeMenuIndex === index;

          return (
            <div
              key={item.localId || item.id || index}
              className={`block-content-item block-content-card-item ${dropIndex === index ? 'drop-before' : ''}`}
              onDragOver={(event) => handleDragOver(event, index)}
              onDrop={(event) => handleDrop(event, getDropIndex(event, index))}
            >
              <div className="block-content-item-row">
                {onMoveItem && (
                  <button
                    type="button"
                    className="block-content-item-action block-content-drag-handle"
                    draggable
                    aria-label={`Reordenar elemento ${index + 1} del bloque`}
                    onDragStart={(event) => {
                      const payload = JSON.stringify({
                        type: 'BLOCK_INTERNAL_SORT',
                        blockIndex,
                        index,
                      });
                      event.dataTransfer.setData('application/x-formquid-block-item', '1');
                      event.dataTransfer.setData('text/plain', payload);
                      event.dataTransfer.effectAllowed = 'move';
                    }}
                    onDragEnd={() => setDropIndex(null)}
                    onKeyDown={(event) => {
                      if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
                      event.preventDefault();
                      onMoveItem(index, event.key === 'ArrowUp' ? index - 1 : index + 2);
                    }}
                  >
                    <MdDragIndicator aria-hidden="true" />
                  </button>
                )}

                <div className="block-content-item-body">
                  {isText ? (
                    <div className="block-content-text">
                      <TextContentIcon />
                      <TextContent data={item} />
                    </div>
                  ) : (
                    <div className="block-content-question">
                      {TypeIcon && (
                        <span
                          className="block-content-question-icon"
                          style={{ color: typeConfig.color, borderColor: `${typeConfig.color}60` }}
                          title={typeConfig.title}
                        >
                          <TypeIcon aria-hidden="true" />
                        </span>
                      )}
                      <Question
                        questionNumber={`${currentQuestionNumber}`}
                        headingLevel={Math.min(normalizedHeadingLevel + 1, 6)}
                        data={questionData}
                        interactive={false}
                      />
                    </div>
                  )}
                </div>

                {(onMoveItem || (!isText && onRequiredChange) || onEditItem || onDeleteItem || onMoveItemOut || onMoveItemToTarget) && (
                  <div className="block-content-item-actions">
                    {onMoveItem && (
                      <>
                        <button
                          type="button"
                          className="block-content-item-action"
                          aria-label={`Mover elemento ${index + 1} del bloque hacia arriba`}
                          disabled={index === 0}
                          onClick={() => onMoveItem(index, index - 1)}
                        >
                          <MdKeyboardArrowUp aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          className="block-content-item-action"
                          aria-label={`Mover elemento ${index + 1} del bloque hacia abajo`}
                          disabled={index === items.length - 1}
                          onClick={() => onMoveItem(index, index + 2)}
                        >
                          <MdKeyboardArrowDown aria-hidden="true" />
                        </button>
                      </>
                    )}
                    {!isText && onRequiredChange && (
                      <RequiredToggle
                        checked={questionData.isRequired}
                        onChange={(isRequired) => onRequiredChange(index, isRequired)}
                      />
                    )}
                    {(onEditItem || onDeleteItem || onMoveItemOut || onMoveItemToTarget) && (
                      <button
                        type="button"
                        className="action-dots-btn"
                        aria-label={isText
                          ? 'Opciones del texto informativo del bloque'
                          : `Opciones de la pregunta ${currentQuestionNumber} del bloque`}
                        onClick={() => setActiveMenuIndex(isMenuOpen ? null : index)}
                      >
                        <MdMoreVert aria-hidden="true" />
                      </button>
                    )}

                    {isMenuOpen && (
                      <div className="actions-floating-menu">
                        {onEditItem && (
                          <button type="button" onClick={() => { setActiveMenuIndex(null); onEditItem(index); }}>
                            <MdEdit aria-hidden="true" />
                            Editar {isText ? 'texto' : 'pregunta'}
                          </button>
                        )}
                        {onMoveItemOut && (
                          <button type="button" onClick={() => { setActiveMenuIndex(null); onMoveItemOut(index); }}>
                            <MdKeyboardArrowRight aria-hidden="true" />
                            Sacar del bloque
                          </button>
                        )}
                        {onMoveItemToTarget && moveTargets.map((target) => (
                          <button key={target.index} type="button" onClick={() => { setActiveMenuIndex(null); onMoveItemToTarget(index, target.index); }}>
                            <MdKeyboardArrowRight aria-hidden="true" />
                            Mover a {target.label}
                          </button>
                        ))}
                        {onDeleteItem && (
                          <button type="button" className="delete-opt" onClick={() => { setActiveMenuIndex(null); onDeleteItem(index); }}>
                            <MdDeleteOutline aria-hidden="true" />
                            Eliminar del bloque
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}

        {onDropQuestion && (
          <div
            className={`block-content-dropzone ${dropIndex === items.length ? 'drop-active' : ''}`}
            onDragOver={(event) => {
              const dragTypes = Array.from(event.dataTransfer.types);
              const isCatalogQuestion = onDropQuestion
                && dragTypes.includes('application/x-formquid-question');
              const isBlockItem = onMoveItem
                && dragTypes.includes('application/x-formquid-block-item');
              const isTemplateQuestion = onDropTemplateQuestion
                && dragTypes.includes('application/x-formquid-template-question');
              if (!isCatalogQuestion && !isBlockItem && !isTemplateQuestion) return;
              event.preventDefault();
              event.stopPropagation();
              onDragInside?.();
              event.dataTransfer.dropEffect = isCatalogQuestion ? 'copy' : 'move';
              setDropIndex(items.length);
            }}
            onDrop={(event) => handleDrop(event, items.length)}
          >
            <div className="block-content-dropzone-text">
              <span className="block-content-dropzone-title">
                <MdAdd aria-hidden="true" />
                Agrega una pregunta
              </span>
              <p>Arrastra aquí una pregunta.</p>
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
