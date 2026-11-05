const SHARED_QUESTION_CATEGORIES = new Set(['DATOS_PERSONALES', 'OTROS']);

export function isCatalogQuestionAllowed(questionCategory, category) {
  return Boolean(category) && (questionCategory === category || SHARED_QUESTION_CATEGORIES.has(questionCategory));
}

export function hasIncompatibleCatalogItem(items = [], nextCategory) {
  return items.some((item) => {
    const catalogCategory = item.catalogCategory ?? item.block?.category;
    const catalogItem = item.fixedQuestionId != null || item.blockId != null || catalogCategory != null;
    const compatible = nextCategory && (item.itemType === 'QUESTION'
      ? isCatalogQuestionAllowed(catalogCategory, nextCategory)
      : catalogCategory === nextCategory);
    return (catalogItem && !compatible) || hasIncompatibleCatalogItem(item.block?.items || [], nextCategory);
  });
}
