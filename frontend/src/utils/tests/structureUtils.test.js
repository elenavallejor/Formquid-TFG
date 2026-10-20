import { describe, expect, it } from 'vitest';

import { INITIAL_QUESTION_STATE } from '../questionBuilderUtils.js';
import { buildCreateCatalogBlockPayload, getBlockBuilderValidationError } from '../blockBuilderUtils.js';
import {
  buildCreateCatalogTemplatePayload, buildTemplateStateFromResponse,
  getTemplateBuilderValidationError, getTemplateItemNumbering,
} from '../templateBuilderUtils.js';
import {
  buildSurveyStructurePayload, buildSurveyStructureState,
  getSurveyPublicationValidationError, getSurveyStructureValidationError,
} from '../surveyBuilderUtils.js';

const question = { ...INITIAL_QUESTION_STATE, itemType: 'QUESTION', statement: '¿Cómo estás?' };

describe('transformaciones de bloques, plantillas y encuestas', () => {
  it('valida que los bloques tengan nombre y preguntas correctas', () => {
    expect(getBlockBuilderValidationError({ name: '', items: [question] })).toMatch(/nombre/);
    expect(getBlockBuilderValidationError({ name: 'Bloque', items: [] })).toMatch(/al menos una pregunta/);
    expect(getBlockBuilderValidationError({ name: 'Bloque', items: [{ itemType: 'TEXT', textBody: ' ' }, question] })).toMatch(/texto no pueden estar vacíos/);
    expect(getBlockBuilderValidationError({ name: 'Bloque', items: [question] })).toBe('');
  });

  it('serializa textos, preguntas propias y fijas en el orden del bloque', () => {
    const payload = buildCreateCatalogBlockPayload({ name: '  Hábitos  ', description: '  Datos  ', items: [
      { itemType: 'TEXT', textBody: '  Introducción  ' },
      question,
      { ...question, fixedQuestionId: 7, isRequired: true },
    ] });
    expect(payload.name).toBe('Hábitos');
    expect(payload.description).toBe('Datos');
    expect(payload.items).toEqual([
      { itemType: 'TEXT', position: 1, textBody: 'Introducción' },
      expect.objectContaining({ itemType: 'QUESTION', position: 2, required: false, question: expect.objectContaining({ statement: '¿Cómo estás?' }) }),
      { itemType: 'QUESTION', position: 3, required: true, fixedQuestionId: 7 },
    ]);
  });

  it('valida la plantilla y conserva la numeración entre preguntas y bloques', () => {
    expect(getTemplateBuilderValidationError({ title: ' ', items: [] })).toMatch(/título/);
    expect(getTemplateBuilderValidationError({ title: 'Plantilla', items: [{ itemType: 'TEXT', textBody: '' }] })).toMatch(/texto no pueden estar vacíos/);
    expect(getTemplateBuilderValidationError({ title: 'Plantilla', items: [{ ...question, statement: '' }] })).toMatch(/Pregunta 1/);
    const items = [question, { itemType: 'BLOCK', block: { name: 'Bloque', items: [question, question] } }, question];
    expect(getTemplateItemNumbering(items)).toEqual([
      { questionNumber: 1 }, { blockLabel: 'A', questionNumberStart: 1 }, { questionNumber: 4 },
    ]);
  });

  it('transforma la respuesta del servidor en un borrador editable', () => {
    const state = buildTemplateStateFromResponse({ title: 'Modelo', contentItems: [
      { itemType: 'TEXT', textBody: 'Inicio' },
      { itemType: 'QUESTION', required: true, question: { id: 4, scope: 'CATALOG_FIXED', statement: '¿Edad?', questionType: 'NUMBER' } },
      { itemType: 'BLOCK', block: { name: 'Bloque', contentItems: [{ itemType: 'TEXT', textBody: 'Dentro' }] } },
    ] });
    expect(state.title).toBe('Modelo');
    expect(state.items[0]).toMatchObject({ itemType: 'TEXT', textBody: 'Inicio' });
    expect(state.items[1]).toMatchObject({ itemType: 'QUESTION', fixedQuestionId: 4, isRequired: true });
    expect(state.items[2].block.items[0].textBody).toBe('Dentro');
    const surveyState = buildSurveyStructureState({ contentItems: [{ itemType: 'QUESTION', question: { id: 4, scope: 'CATALOG_FIXED', statement: '¿Edad?', questionType: 'NUMBER' } }] });
    expect(surveyState.items[0].fixedQuestionId).toBeUndefined();
  });

  it('prepara el contenido ordenado de plantillas y encuestas', () => {
    const data = { title: '  Plantilla  ', description: '  Descripción  ', items: [
      { itemType: 'TEXT', textBody: '  Inicio  ' },
      { ...question, isRequired: true },
      { itemType: 'BLOCK', blockId: 8 },
    ] };
    const template = buildCreateCatalogTemplatePayload(data);
    expect(template.title).toBe('Plantilla');
    expect(template.description).toBe('Descripción');
    expect(template.items[0]).toEqual({ itemType: 'TEXT', position: 1, textBody: 'Inicio' });
    expect(template.items[1]).toMatchObject({ itemType: 'QUESTION', position: 2, required: true, question: expect.objectContaining({ statement: '¿Cómo estás?' }) });
    expect(template.items[2]).toEqual({ itemType: 'BLOCK', position: 3, blockId: 8 });
    expect(buildSurveyStructurePayload({ items: data.items }).items).toHaveLength(3);
  });

  it('exige al menos una pregunta antes de publicar una encuesta', () => {
    expect(getSurveyStructureValidationError({ items: [{ itemType: 'TEXT', textBody: '' }] })).toMatch(/texto no pueden estar vacíos/);
    expect(getSurveyPublicationValidationError({ items: [{ itemType: 'TEXT', textBody: 'Información' }] })).toMatch(/al menos una pregunta/);
    expect(getSurveyPublicationValidationError({ items: [question] })).toBe('');
  });
});
