import {
  buildBlockQuestionFromCatalog,
  buildCreateCatalogBlockPayload,
  getBlockBuilderValidationError,
  getBlockItems,
} from './blockBuilderUtils.js';
import { buildQuestionDraftPayload, getQuestionBuilderValidationError } from './questionBuilderUtils.js';

export const INITIAL_TEMPLATE_STATE = {
  title: '',
  description: '',
  items: [],
};

function createLocalId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export function buildTemplateQuestionFromCatalog(question) {
  return {
    ...buildBlockQuestionFromCatalog(question),
    itemType: 'QUESTION',
    localId: createLocalId('question'),
    fixedQuestionId: question.id,
  };
}

export function buildTemplateBlockFromCatalog(block) {
  return {
    itemType: 'BLOCK',
    localId: createLocalId('block'),
    blockId: block.id,
    block,
  };
}

export function buildEditableTemplateBlock(block) {
  const sourceItems = Array.isArray(block?.contentItems)
    ? block.contentItems
    : getBlockItems(block);

  return {
    name: block?.name || '',
    description: block?.description || '',
    items: sourceItems.map((item) => {
      if (item.itemType === 'TEXT') {
        return {
          itemType: 'TEXT',
          localId: item.localId || createLocalId('block-text'),
          textBody: item.textBody,
        };
      }

      return {
        ...buildBlockQuestionFromCatalog(item.question || item),
        itemType: 'QUESTION',
        localId: item.localId || createLocalId('block-question'),
        isRequired: Boolean(item.required ?? item.isRequired),
      };
    }),
  };
}

export function addCatalogQuestionToTemplateBlock(templateBlock, question, targetIndex) {
  const block = buildEditableTemplateBlock(templateBlock.block || templateBlock);
  const items = [...block.items];
  const insertionIndex = Number.isInteger(targetIndex)
    ? Math.max(0, Math.min(targetIndex, items.length))
    : items.length;
  items.splice(insertionIndex, 0, {
    ...buildBlockQuestionFromCatalog(question),
    itemType: 'QUESTION',
    localId: createLocalId('block-question'),
  });

  const localBlock = { ...templateBlock };
  delete localBlock.blockId;
  delete localBlock.questionCount;

  return {
    ...localBlock,
    block: {
      ...block,
      items,
    },
  };
}

export function getTemplateBuilderValidationError(data) {
  if (!data?.title?.trim()) return 'Debes asignar un título a la plantilla.';

  let questionNumber = 0;
  for (const item of data.items || []) {
    if (item.itemType === 'TEXT' && !item.textBody?.trim()) {
      return 'Los elementos de texto no pueden estar vacíos.';
    }
    if (item.itemType === 'QUESTION') {
      questionNumber += 1;
    }
    if (item.itemType === 'QUESTION' && !item.fixedQuestionId) {
      const questionError = getQuestionBuilderValidationError(item);
      if (questionError) return `Pregunta ${questionNumber}: ${questionError}`;
    }
    if (item.itemType === 'BLOCK' && item.block && !item.blockId) {
      const blockError = getBlockBuilderValidationError(item.block);
      if (blockError) return `Bloque «${item.block.name || 'sin nombre'}»: ${blockError}`;
    }
  }

  return '';
}

export function buildCreateCatalogTemplatePayload(data) {
  return {
    title: data.title.trim(),
    description: data.description?.trim() || null,
    imageKey: null,
    items: (data.items || []).map((item, index) => {
      const baseItem = {
        itemType: item.itemType,
        position: index + 1,
      };

      if (item.itemType === 'TEXT') {
        return { ...baseItem, textBody: item.textBody.trim() };
      }
      if (item.itemType === 'BLOCK') {
        return item.blockId
          ? { ...baseItem, blockId: item.blockId }
          : { ...baseItem, block: buildCreateCatalogBlockPayload(item.block) };
      }
      if (item.fixedQuestionId) {
        return {
          ...baseItem,
          required: Boolean(item.isRequired),
          fixedQuestionId: item.fixedQuestionId,
        };
      }
      return {
        ...baseItem,
        required: Boolean(item.isRequired),
        question: buildQuestionDraftPayload(item),
      };
    }),
  };
}

export function getTemplateBlockQuestionCount(block) {
  return buildEditableTemplateBlock(block).items.filter((item) => item.itemType === 'QUESTION').length;
}
