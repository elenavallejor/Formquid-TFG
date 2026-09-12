/**
 * Proporciona operaciones de arrastre y ordenación compartidas entre constructores.
 */
export function getDragData(event) {
  const value = event.dataTransfer.getData('application/x-formquid-catalog-item') || event.dataTransfer.getData('text/plain');

  if (!value) return null;

  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

export function getDropIndex(event, itemIndex) {
  const bounds = event.currentTarget.getBoundingClientRect();
  return event.clientY < bounds.top + bounds.height / 2 ? itemIndex : itemIndex + 1;
}

export function reorderItems(items, fromIndex, toIndex) {
  if (!Number.isInteger(fromIndex) || !Number.isInteger(toIndex) || fromIndex < 0 || fromIndex >= items.length) {
    return items;
  }

  const targetIndex = Math.max(0, Math.min(toIndex, items.length));
  const insertionIndex = fromIndex < targetIndex ? targetIndex - 1 : targetIndex;

  if (insertionIndex === fromIndex) return items;

  const updated = [...items];
  const [item] = updated.splice(fromIndex, 1);
  updated.splice(insertionIndex, 0, item);
  return updated;
}