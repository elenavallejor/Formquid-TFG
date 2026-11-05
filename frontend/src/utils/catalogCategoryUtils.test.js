import { describe, expect, it } from 'vitest';

import { hasIncompatibleCatalogItem, isCatalogQuestionAllowed } from './catalogCategoryUtils.js';

describe('hasIncompatibleCatalogItem', () => {
  it('detecta preguntas fijas y bloques del catálogo, también dentro de bloques locales', () => {
    expect(hasIncompatibleCatalogItem([{ itemType: 'TEXT', textBody: 'Contexto' }], 'SALUD')).toBe(false);
    expect(hasIncompatibleCatalogItem([{ itemType: 'QUESTION', fixedQuestionId: 4 }], 'SALUD')).toBe(true);
    expect(hasIncompatibleCatalogItem([{ itemType: 'BLOCK', blockId: 5 }], 'SALUD')).toBe(true);
    expect(hasIncompatibleCatalogItem([{ itemType: 'BLOCK', block: { items: [{ itemType: 'QUESTION', catalogCategory: 'SALUD' }] } }], 'TECNOLOGIA')).toBe(true);
  });
});


describe('categorías compartidas', () => {
  it('permite preguntas de datos personales y otros en cualquier categoría elegida', () => {
    expect(isCatalogQuestionAllowed('DATOS_PERSONALES', 'SALUD')).toBe(true);
    expect(isCatalogQuestionAllowed('OTROS', 'SALUD')).toBe(true);
    expect(isCatalogQuestionAllowed('TECNOLOGIA', 'SALUD')).toBe(false);
    expect(isCatalogQuestionAllowed('OTROS', '')).toBe(false);
  });

  it('deja cambiar la categoría con preguntas compartidas, pero protege las incompatibles', () => {
    expect(hasIncompatibleCatalogItem([{ itemType: 'QUESTION', fixedQuestionId: 1, catalogCategory: 'DATOS_PERSONALES' }], 'SALUD')).toBe(false);
    expect(hasIncompatibleCatalogItem([{ itemType: 'QUESTION', fixedQuestionId: 2, catalogCategory: 'OTROS' }], 'SALUD')).toBe(false);
    expect(hasIncompatibleCatalogItem([{ itemType: 'QUESTION', fixedQuestionId: 3, catalogCategory: 'TECNOLOGIA' }], 'SALUD')).toBe(true);
    expect(hasIncompatibleCatalogItem([{ itemType: 'BLOCK', blockId: 4, block: { category: 'OTROS' } }], 'SALUD')).toBe(true);
  });
});
