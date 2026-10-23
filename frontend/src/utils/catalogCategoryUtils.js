export function containsCatalogItem(items = []) {
  return items.some((item) => item.fixedQuestionId != null
    || item.blockId != null
    || item.catalogCategory != null
    || containsCatalogItem(item.block?.items || []));
}
