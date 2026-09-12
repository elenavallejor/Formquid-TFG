import { useState } from 'react';
import { MdAdd, MdDeleteOutline, MdDragIndicator, MdEdit } from 'react-icons/md';

import { buildBlockQuestionFromCatalog, getBlockItems } from '../../utils/blockBuilderUtils.js';
import { getDragData, getDropIndex } from '../../utils/builderDragUtils.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';
import { Question } from '../Question/Question.jsx';
import { RequiredToggle } from '../RequiredToggle/RequiredToggle.jsx';
import { TextContent } from '../TextContent/TextContent.jsx';

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
  onDropQuestion,
  onDropTemplateQuestion,
  onDropBlockQuestion,
  onMoveItem,
  onEditItem,
  onDeleteItem,
  onRequiredChange,
  onDragInside,
}) {
  const [dropIndex, setDropIndex] = useState(null);
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
      onDropBlockQuestion?.(dragData.blockIndex, dragData.index, targetIndex);
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
        <h3>{data.name || 'Bloque sin nombre'}</h3>
        {data.description && <p>{data.description}</p>}
      </header>

      <div className="block-content-items">
        {items.map((item, index) => {
          const isText = item.itemType === 'TEXT';
          const currentQuestionNumber = isText ? null : ++questionNumber;
          const questionData = isText ? null : getQuestionData(item);
          const typeConfig = isText
            ? null
            : QUESTION_TYPES.find((type) => type.id === questionData.questionType);
          const TypeIcon = typeConfig?.icon;

          return (
            <div
              key={item.localId || item.id || index}
              className={`block-content-item ${isText ? '' : 'block-content-question-item'} ${dropIndex === index ? 'drop-before' : ''}`}
              onDragOver={(event) => handleDragOver(event, index)}
              onDrop={(event) => handleDrop(event, getDropIndex(event, index))}
            >
              <div className="block-content-item-row">
                {!isText && onMoveItem && (
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
                    <TextContent data={item} />
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
                        data={questionData}
                        interactive={false}
                      />
                    </div>
                  )}
                </div>

                {!isText && (onRequiredChange || onEditItem || onDeleteItem) && (
                  <div className="block-content-item-actions">
                    {onRequiredChange && (
                      <RequiredToggle
                        checked={questionData.isRequired}
                        onChange={(isRequired) => onRequiredChange(index, isRequired)}
                      />
                    )}
                    {onEditItem && (
                      <button
                        type="button"
                        className="block-content-item-action"
                        aria-label={`Editar pregunta ${currentQuestionNumber} del bloque`}
                        onClick={() => onEditItem(index)}
                      >
                        <MdEdit aria-hidden="true" />
                      </button>
                    )}
                    {onDeleteItem && (
                      <button
                        type="button"
                        className="block-content-item-action block-content-delete"
                        aria-label={`Eliminar pregunta ${currentQuestionNumber} del bloque`}
                        onClick={() => onDeleteItem(index)}
                      >
                        <MdDeleteOutline aria-hidden="true" />
                      </button>
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
