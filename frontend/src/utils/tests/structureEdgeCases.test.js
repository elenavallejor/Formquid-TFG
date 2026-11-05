import { describe, it, expect } from 'vitest';
import * as surveys from '../surveyBuilderUtils.js';
import * as templates from '../templateBuilderUtils.js';
import { INITIAL_QUESTION_STATE } from '../questionBuilderUtils.js';
import { getNextRole, getUserRoleLabel, getVisiblePages, canChangeUserRole, canDeleteUser } from '../usersManagementUtils.js';
const q = { ...INITIAL_QUESTION_STATE, itemType: 'QUESTION', statement: 'Pregunta' };

describe('estructuras con bloques y datos opcionales', () => {
  it('trata la ausencia de contenido como estructura vacía', () => {
    expect(surveys.buildSurveyStructureState()).toEqual({ items: [] });
    expect(surveys.buildSurveyStructurePayload()).toEqual({ items: [] });
    expect(surveys.getSurveyStructureValidationError()).toBe('');
    expect(surveys.getSurveyPublicationValidationError()).toMatch(/al menos una pregunta/);
    expect(templates.getTemplateBuilderValidationError()).toMatch(/título/);
    expect(templates.getTemplateBuilderValidationError({ title: 'Título' })).toBe('');
    expect(templates.getTemplateItemNumbering()).toEqual([]);
  });
  it('convierte preguntas fijas de bloques en preguntas editables sin perder textos', () => {
    const state = surveys.buildSurveyStructureState({ contentItems: [
      { itemType: 'TEXT' },
      { itemType: 'BLOCK', block: { contentItems: [
        { itemType: 'TEXT', textBody: 'Nota', localId: 'text' },
        { itemType: 'QUESTION', required: true, question: { id: 4, scope: 'CATALOG_FIXED', statement: 'Fija', questionType: 'TEXT' } },
      ] } },
    ] });
    expect(state.items[0].textBody).toBe('');
    expect(state.items[1].block.items[1]).not.toHaveProperty('fixedQuestionId');
    expect(state.items[1].block.items[1].isRequired).toBe(true);
    expect(state.items[1].block.items[0].textBody).toBe('Nota');
  });
  it.each(['items', 'contentItems'])('reconoce preguntas publicables en bloques con %s', (key) => {
    expect(surveys.getSurveyPublicationValidationError({ items: [{ itemType: 'BLOCK', blockId: 1, block: { [key]: [q] } }] })).toBe('');
  });
  it('rechaza bloques inválidos y conserva referencias de catálogo al serializar', () => {
    expect(templates.getTemplateBuilderValidationError({ title: 'T', items: [{ itemType: 'BLOCK', block: { items: [] } }] })).toMatch(/sin nombre/);
    const payload = templates.buildCreateCatalogTemplatePayload({ title: 'T', items: [
      { ...q, fixedQuestionId: 2 },
      { itemType: 'BLOCK', block: { name: 'B', items: [q] } },
    ] });
    expect(payload.description).toBeNull();
    expect(payload.items[0].fixedQuestionId).toBe(2);
    expect(payload.items[1].block.name).toBe('B');
  });
  it.each([-1, 0, 1, 20, undefined])('inserta en una posición válida de un bloque: %s', (index) => {
    const block = templates.addCatalogQuestionToTemplateBlock({ blockId: 8, block: { name: 'B', items: [{ itemType: 'TEXT', textBody: 'Nota' }] } }, { id: 4, statement: 'Nueva', questionType: 'TEXT' }, index);
    expect(block).not.toHaveProperty('blockId');
    expect(block.block.items).toHaveLength(2);
    expect(block.block.items.find(item => item.itemType === 'QUESTION').statement).toBe('Nueva');
  });
  it('numera bloques después de Z y respeta textos', () => {
    const numbers = templates.getTemplateItemNumbering([{ itemType: 'TEXT' }, ...Array.from({ length: 27 }, () => ({ itemType: 'BLOCK', contentItems: [q] }))]);
    expect(numbers[0]).toEqual({});
    expect(numbers.at(-1)).toEqual({ blockLabel: 'AA', questionNumberStart: 26 });
  });
});
describe('permisos y paginación de usuarios', () => {
  it.each([['EDITOR', 'ADMIN'], ['ADMIN', 'EDITOR'], ['SUPERADMIN', 'ADMIN'], ['UNKNOWN', null]])('resuelve la transición de %s', (role, next) => expect(getNextRole(role)).toBe(next));
  it('protege el rol propio y la eliminación de superadministradores', () => {
    expect(canChangeUserRole({ id: 1 }, { id: 1 })).toBe(false);
    expect(canChangeUserRole({ id: 2, role: 'SUPERADMIN' }, { id: 1 })).toBe(true);
    expect(canDeleteUser({ id: 2, role: 'SUPERADMIN' }, { id: 1 })).toBe(false);
    expect(canDeleteUser({ id: 2, role: 'ADMIN' }, { id: 1 })).toBe(true);
    expect(getUserRoleLabel()).toBe('Sin rol');
    expect(getUserRoleLabel('EXTERNAL')).toBe('EXTERNAL');
  });
  it('limita las páginas visibles al principio y al final', () => {
    expect(getVisiblePages(0, 0)).toEqual([]);
    expect(getVisiblePages(0, 3)).toEqual([0, 1, 2]);
    expect(getVisiblePages(0, 9)).toEqual([0, 1, 2, 3, 4]);
    expect(getVisiblePages(8, 9)).toEqual([4, 5, 6, 7, 8]);
  });
});
