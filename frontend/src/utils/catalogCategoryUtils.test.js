import { describe, expect, it } from 'vitest';

import { containsCatalogItem } from './catalogCategoryUtils.js';

describe('containsCatalogItem', () => {
  it('detecta preguntas fijas y bloques del catálogo, también dentro de bloques locales', () => {
    expect(containsCatalogItem([{ itemType: 'TEXT', textBody: 'Contexto' }])).toBe(false);
    expect(containsCatalogItem([{ itemType: 'QUESTION', fixedQuestionId: 4 }])).toBe(true);
    expect(containsCatalogItem([{ itemType: 'BLOCK', blockId: 5 }])).toBe(true);
    expect(containsCatalogItem([{ itemType: 'BLOCK', block: { items: [{ itemType: 'QUESTION', catalogCategory: 'SALUD' }] } }])).toBe(true);
  });
});
