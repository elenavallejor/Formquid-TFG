export function normalizeTag(value) {
  let normalized = value.trim().toLocaleLowerCase('es');
  if (normalized.startsWith('#')) normalized = normalized.slice(1);
  return normalized.normalize('NFKC').replace(/\s+/g, '-');
}
