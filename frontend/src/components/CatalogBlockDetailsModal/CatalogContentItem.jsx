import { MdLayers } from 'react-icons/md';

import { Block } from '../Block/Block.jsx';
import { Question } from '../Question/Question.jsx';
import { TextContent, TextContentIcon } from '../TextContent/TextContent.jsx';
import { buildBlockQuestionFromCatalog } from '../../utils/blockBuilderUtils.js';
import { QUESTION_TYPES } from '../../utils/questionBuilderUtils.js';

export function CatalogContentItem({ item, numbering = {} }) {
  const isText = item.itemType === 'TEXT';
  const isBlock = item.itemType === 'BLOCK';
  const questionData = !isText && !isBlock
    ? {
        ...buildBlockQuestionFromCatalog(item.question || item),
        isRequired: Boolean(item.required ?? item.isRequired),
      }
    : null;
  const typeConfig = questionData
    ? QUESTION_TYPES.find((type) => type.id === questionData.questionType)
    : null;
  const TypeIcon = typeConfig?.icon;

  return (
    <div className={`block-question-item catalog-detail-builder-item ${isBlock ? 'template-block-item' : ''}`}>
      <div className="question-item-left">
        {isText ? (
          <TextContentIcon />
        ) : isBlock ? (
          <span className="question-type-badge template-block-badge" title="Bloque de preguntas">
            <MdLayers aria-hidden="true" />
          </span>
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
        ) : isBlock ? (
          <Block
            data={item.block}
            blockLabel={numbering.blockLabel}
            headingLevel={4}
            questionNumberStart={numbering.questionNumberStart}
          />
        ) : (
          <Question
            questionNumber={`${numbering.questionNumber}`}
            headingLevel={4}
            data={questionData}
            interactive={false}
          />
        )}
      </div>
    </div>
  );
}
