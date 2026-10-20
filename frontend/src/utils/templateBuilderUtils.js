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

export function buildTemplateStateFromResponse(template) {
  return {
    title: template?.title || '',
    description: template?.description || '',
    items: (template?.contentItems || []).map((item) => {
      if (item.itemType === 'TEXT') {
        return {
          itemType: 'TEXT',
          localId: createLocalId('template-text'),
          textBody: item.textBody || '',
        };
      }

      if (item.itemType === 'BLOCK') {
        return {
          itemType: 'BLOCK',
          localId: createLocalId('template-block'),
          block: buildEditableTemplateBlock(item.block),
        };
      }

      return {
        ...buildBlockQuestionFromCatalog(item.question),
        itemType: 'QUESTION',
        localId: createLocalId('template-question'),
        isRequired: Boolean(item.required),
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

export function getTemplateBuilderValidationError(data, { includeEvaluation = false } = {}) {
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
      const questionError = getQuestionBuilderValidationError(item, { includeEvaluation });
      if (questionError) return `Pregunta ${questionNumber}: ${questionError}`;
    }
    if (item.itemType === 'BLOCK' && item.block && !item.blockId) {
      const blockError = getBlockBuilderValidationError(item.block, { includeEvaluation });
      if (blockError) return `Bloque «${item.block.name || 'sin nombre'}»: ${blockError}`;
    }
  }

  return '';
}

export function buildCreateCatalogTemplatePayload(data, { includeEvaluation = false } = {}) {
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
          : { ...baseItem, block: buildCreateCatalogBlockPayload(item.block, { includeEvaluation }) };
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
        question: buildQuestionDraftPayload(item, { includeEvaluation }),
      };
    }),
  };
}

function getTemplateBlockQuestionCount(block) {
  const items = Array.isArray(block?.contentItems)
    ? block.contentItems
    : getBlockItems(block);

  return items.filter((item) => item.itemType === 'QUESTION').length;
}

export function getTemplateItemNumbering(items = []) {
  let questionNumber = 0;
  let blockNumber = 0;

  return items.map((item) => {
    if (item.itemType === 'QUESTION') {
      questionNumber += 1;
      return { questionNumber };
    }

    if (item.itemType === 'BLOCK') {
      blockNumber += 1;
      const questionNumberStart = questionNumber;
      questionNumber += getTemplateBlockQuestionCount(item.block || item);

      return {
        blockLabel: getAlphabeticLabel(blockNumber),
        questionNumberStart,
      };
    }

    return {};
  });
}

function getAlphabeticLabel(position) {
  let value = position;
  let label = '';

  while (value > 0) {
    value -= 1;
    label = String.fromCharCode(65 + (value % 26)) + label;
    value = Math.floor(value / 26);
  }

  return label;
}
