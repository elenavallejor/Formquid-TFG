import {
  buildCreateCatalogTemplatePayload,
  buildTemplateStateFromResponse,
  getTemplateBuilderValidationError,
} from './templateBuilderUtils.js';

export const INITIAL_SURVEY_STRUCTURE = {
  items: [],
};

export function buildSurveyStructureState(survey) {
  const templateState = buildTemplateStateFromResponse(survey);
  return {
    items: templateState.items.map((item) => {
      if (item.itemType === 'QUESTION') {
        const editableQuestion = { ...item };
        delete editableQuestion.fixedQuestionId;
        return editableQuestion;
      }
      if (item.itemType !== 'BLOCK') return item;

      return {
        ...item,
        block: {
          ...item.block,
          items: (item.block?.items || []).map((blockItem) => {
            if (blockItem.itemType !== 'QUESTION') return blockItem;
            const editableQuestion = { ...blockItem };
            delete editableQuestion.fixedQuestionId;
            return editableQuestion;
          }),
        },
      };
    }),
  };
}

export function getSurveyStructureValidationError(data) {
  return getTemplateBuilderValidationError({
    title: 'Encuesta',
    description: '',
    items: data?.items || [],
  }, { includeEvaluation: true });
}

export function getSurveyPublicationValidationError(data) {
  const structureError = getSurveyStructureValidationError(data);
  if (structureError) return structureError;
  const hasQuestion = (data?.items || []).some((item) => (
    item.itemType === 'QUESTION'
    || (item.itemType === 'BLOCK' && (item.block?.items || item.block?.contentItems || []).some((blockItem) => blockItem.itemType === 'QUESTION'))
  ));
  return hasQuestion ? '' : 'La encuesta necesita al menos una pregunta antes de publicarse.';
}

export function buildSurveyStructurePayload(data) {
  return {
    items: buildCreateCatalogTemplatePayload({
      title: 'Encuesta',
      description: null,
      items: data?.items || [],
    }, { includeEvaluation: true }).items,
  };
}
