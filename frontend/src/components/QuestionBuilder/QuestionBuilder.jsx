import { MdAdd, MdDeleteOutline, MdOutlineThumbDown, MdOutlineThumbUp } from 'react-icons/md';

import {
  CHOICE_QUESTION_TYPES,
  QUESTION_TYPES,
  RANGE_QUESTION_TYPES,
  SCALE_DISPLAY_OPTIONS,
  getMatrixRowText,
  getOptionFactor,
  getOptionText,
  getScaleDisplayValue,
  parseIntegerValue,
  parseNumberValue,
} from '../../utils/questionBuilderUtils.js';
import { Question } from '../Question/Question.jsx';

import './QuestionBuilder.css';

export function QuestionBuilder({ data, onChange, isCatalog = true }) {
  const isTextQuestion = data.questionType === 'TEXT';
  const scaleDisplayType = getScaleDisplayValue(data);
  const isFixedVisualScale = scaleDisplayType === 'STARS' || scaleDisplayType === 'FACES';

  function handleField(field, value) {
    onChange({ ...data, [field]: value });
  }

  function handleQuestionType(questionType) {
    const nextData = { ...data, questionType };
    if (questionType === 'TEXT') {
      nextData.evaluationDirection = 'NEUTRAL';
      nextData.evaluationWeight = null;
    }
    onChange(nextData);
  }

  function handleEvaluationDirection(evaluationDirection) {
    if (isTextQuestion && evaluationDirection !== 'NEUTRAL') return;
    const nextData = { ...data, evaluationDirection };
    if (evaluationDirection === 'NEUTRAL') {
      nextData.evaluationWeight = null;
    } else {
      nextData.evaluationWeight = data.evaluationWeight || 1;
      nextData.options = (data.options || []).map((option, index) => ({
        ...option,
        evaluationFactor: getOptionFactor(option) === '' ? (index === 0 ? 1 : 0) : getOptionFactor(option),
      }));
      nextData.trueFactor = data.trueFactor === '' || data.trueFactor === null || data.trueFactor === undefined ? 1 : data.trueFactor;
      nextData.falseFactor = data.falseFactor === '' || data.falseFactor === null || data.falseFactor === undefined ? 0 : data.falseFactor;
    }
    onChange(nextData);
  }

  function handleScaleDisplayType(value) {
    const nextData = { ...data, scaleDisplayType: value };
    if (value === 'STARS' || value === 'FACES') {
      nextData.minValue = 1;
      nextData.maxValue = 5;
      nextData.step = 1;
    }
    onChange(nextData);
  }

  function handleOptionChange(index, field, value) {
    const updated = [...(data.options || [])];
    updated[index] = { ...(updated[index] || {}), [field]: value };
    handleField('options', updated);
  }

  function addOption() {
    const currentOptions = data.options || [];
    handleField('options', [
      ...currentOptions,
      {
        label: `Opción ${currentOptions.length + 1}`,
        evaluationFactor: data.evaluationDirection === 'NEUTRAL' ? null : 0,
      },
    ]);
  }

  function removeOption(index) {
    handleField('options', (data.options || []).filter((_, currentIndex) => currentIndex !== index));
  }

  function handleMatrixRowChange(index, value) {
    const updated = [...(data.matrixRows || [])];
    updated[index] = { ...(typeof updated[index] === 'string' ? {} : updated[index]), label: value };
    handleField('matrixRows', updated);
  }

  function addMatrixRow() {
    const currentRows = data.matrixRows || [];
    handleField('matrixRows', [...currentRows, { label: `Fila ${currentRows.length + 1}` }]);
  }

  function removeMatrixRow(index) {
    handleField('matrixRows', (data.matrixRows || []).filter((_, currentIndex) => currentIndex !== index));
  }

  return (
    <div className="question-builder-stack">
      <section className="builder-card">
        <h2 className="builder-section-title">INFORMACIÓN GENERAL</h2>

        {isCatalog && (
          <div className="builder-field">
            <label htmlFor="catalogName">Nombre en el catálogo <span>*</span></label>
            <input
              id="catalogName"
              type="text"
              placeholder="Nombre corto para identificar la pregunta en el catálogo."
              maxLength={150}
              value={data.catalogName || ''}
              onChange={(event) => handleField('catalogName', event.target.value)}
            />
            <span className="field-counter">{(data.catalogName || '').length}/150</span>
          </div>
        )}

        <div className="builder-field">
          <label htmlFor="statement">Enunciado <span>*</span></label>
          <input
            id="statement"
            type="text"
            placeholder="Texto que verá el encuestado."
            maxLength={300}
            value={data.statement || ''}
            onChange={(event) => handleField('statement', event.target.value)}
          />
          <span className="field-counter">{(data.statement || '').length}/300</span>
        </div>

        <div className="builder-field">
          <label htmlFor="annotation">Nota (opcional)</label>
          <textarea
            id="annotation"
            placeholder="Agrega una nota o contexto adicional para la pregunta..."
            maxLength={300}
            rows={3}
            value={data.annotation || ''}
            onChange={(event) => handleField('annotation', event.target.value)}
          />
          <span className="field-counter">{(data.annotation || '').length}/300</span>
        </div>
      </section>

      <section className="builder-card">
        <h2 className="builder-section-title">TIPO DE PREGUNTA</h2>
        <p className="builder-section-desc">Selecciona el tipo de respuesta que tendrá esta pregunta.</p>

        <div className="question-types-grid">
          {QUESTION_TYPES.map((type) => {
            const Icon = type.icon;
            const isSelected = data.questionType === type.id;

            return (
              <button
                key={type.id}
                type="button"
                className={`type-tile ${isSelected ? 'selected' : ''}`}
                aria-pressed={isSelected}
                onClick={() => handleQuestionType(type.id)}
              >
                <span
                  className="tile-icon-box"
                  style={{
                    color: type.color,
                    borderColor: `${type.color}80`,
                  }}
                >
                  <Icon aria-hidden="true" />
                </span>
                <span className="tile-title">{type.title}</span>
                <span className="tile-desc">{type.desc}</span>
              </button>
            );
          })}
        </div>
      </section>

      <section className="builder-card">
        <h2 className="builder-section-title">CONFIGURACIÓN DE LA PREGUNTA</h2>

        {!isCatalog && (
          <div className="config-grid-row">
            <div className="config-label-cell">Obligatoriedad:</div>
            <div className="config-interactive-cell">
              <label className="checkbox-sublabel">
                <input
                  type="checkbox"
                  checked={Boolean(data.isRequired)}
                  onChange={(event) => handleField('isRequired', event.target.checked)}
                />
                <span>Respuesta obligatoria para el encuestado</span>
              </label>
            </div>
            <div className="config-help-cell">Impide enviar el cuestionario si la pregunta queda sin responder.</div>
          </div>
        )}

        <div className="config-grid-row">
          <div className="config-label-cell">Dirección de evaluación:</div>
          <div className="config-interactive-cell">
            <div className="direction-btn-group">
              <button
                type="button"
                className={data.evaluationDirection === 'POSITIVE' ? 'active' : ''}
                aria-pressed={data.evaluationDirection === 'POSITIVE'}
                disabled={isTextQuestion}
                onClick={() => handleEvaluationDirection('POSITIVE')}
              >
                <MdOutlineThumbUp aria-hidden="true" />
                Positiva
              </button>
              <button
                type="button"
                className={data.evaluationDirection === 'NEUTRAL' ? 'active' : ''}
                aria-pressed={data.evaluationDirection === 'NEUTRAL'}
                onClick={() => handleEvaluationDirection('NEUTRAL')}
              >
                Neutral
              </button>
              <button
                type="button"
                className={data.evaluationDirection === 'NEGATIVE' ? 'active' : ''}
                aria-pressed={data.evaluationDirection === 'NEGATIVE'}
                disabled={isTextQuestion}
                onClick={() => handleEvaluationDirection('NEGATIVE')}
              >
                <MdOutlineThumbDown aria-hidden="true" />
                Negativa
              </button>
            </div>
          </div>
          <div className="config-help-cell">Indica si la respuesta se interpreta como favorable, desfavorable o solo informativa respecto al tema de investigación.</div>
        </div>

        {data.evaluationDirection !== 'NEUTRAL' && (
          <div className="config-grid-row">
            <div className="config-label-cell">Peso de la pregunta:</div>
            <div className="config-interactive-cell">
              <input
                type="number"
                step="0.1"
                placeholder="Ej. 1,5"
                value={data.evaluationWeight ?? ''}
                onChange={(event) => handleField('evaluationWeight', parseNumberValue(event.target.value))}
              />
            </div>
            <div className="config-help-cell">Indica la importancia de esta pregunta dentro del cálculo de resultados del estudio.</div>
          </div>
        )}

        {data.questionType === 'TEXT' && (
          <div className="config-grid-row">
            <div className="config-label-cell">Longitud máxima del texto:</div>
            <div className="config-interactive-cell">
              <input
                type="number"
                min="1"
                placeholder="Ej. 300 caracteres."
                value={data.maxLength ?? ''}
                onChange={(event) => handleField('maxLength', parseIntegerValue(event.target.value))}
              />
            </div>
            <div className="config-help-cell">Límite de caracteres para la respuesta escrita.</div>
          </div>
        )}

        {RANGE_QUESTION_TYPES.includes(data.questionType) && (
          <>
            <div className="config-grid-row">
              <div className="config-label-cell">
                {data.questionType === 'NUMBER' ? 'Rango permitido:' : 'Rango:'}
              </div>
              <div className="config-interactive-cell limits-labeled-group">
                <div className="labeled-input-item">
                  <span className="mini-top-label">Mínimo</span>
                  <input
                    type="number"
                    value={isFixedVisualScale ? 1 : (data.minValue ?? '')}
                    disabled={isFixedVisualScale}
                    onChange={(event) => handleField('minValue', parseNumberValue(event.target.value))}
                    placeholder="1"
                  />
                </div>
                <div className="labeled-input-item">
                  <span className="mini-top-label">Máximo</span>
                  <input
                    type="number"
                    value={isFixedVisualScale ? 5 : (data.maxValue ?? '')}
                    disabled={isFixedVisualScale}
                    onChange={(event) => handleField('maxValue', parseNumberValue(event.target.value))}
                    placeholder="5"
                  />
                </div>

                {data.questionType !== 'NUMBER' && (
                  <div className="labeled-input-item">
                    <span className="mini-top-label">Paso</span>
                    <input
                      type="number"
                      value={isFixedVisualScale ? 1 : (data.step ?? '')}
                      disabled={isFixedVisualScale}
                      onChange={(event) => handleField('step', parseNumberValue(event.target.value))}
                      placeholder="1"
                    />
                  </div>
                )}
              </div>
              <div className="config-help-cell">
                {data.questionType === 'NUMBER'
                  ? 'Valores numéricos mínimo y máximo permitidos para la respuesta.'
                  : isFixedVisualScale
                    ? 'Fijado de 1 a 5 con incremento de 1 para este formato visual.'
                    : 'Valores que delimitan la escala. El mínimo debe ser inferior al máximo y el paso mayor que cero.'}
              </div>
            </div>

            {data.questionType !== 'NUMBER' && (
              <>
                <div className="config-grid-row config-row-wide">
                  <div className="config-label-cell">Etiquetas:</div>
                  <div className="config-interactive-cell limits-labeled-group">
                    <div className="labeled-input-item">
                      <span className="mini-top-label">Etiqueta inicial</span>
                      <input
                        type="text"
                        value={data.leftLabel || ''}
                        placeholder="Ej. Totalmente en desacuerdo"
                        onChange={(event) => handleField('leftLabel', event.target.value)}
                      />
                    </div>
                    <div className="labeled-input-item">
                      <span className="mini-top-label">Etiqueta final</span>
                      <input
                        type="text"
                        value={data.rightLabel || ''}
                        placeholder="Ej. Totalmente de acuerdo"
                        onChange={(event) => handleField('rightLabel', event.target.value)}
                      />
                    </div>
                  </div>
                  <div className="config-help-cell">Textos que explican los extremos de la escala.</div>
                </div>

                <div className="config-grid-row">
                  <div className="config-label-cell">Tipo visual:</div>
                  <div className="config-interactive-cell">
                    <select value={getScaleDisplayValue(data)} onChange={(event) => handleScaleDisplayType(event.target.value)}>
                      {SCALE_DISPLAY_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="config-help-cell">Forma en la que se mostrará la escala al encuestado.</div>
                </div>
              </>
            )}

            {data.questionType === 'MATRIX_SCALE' && (
              <div className="config-grid-row">
                <div className="config-label-cell">Filas de matriz:</div>
                <div className="config-interactive-cell full-width-field">
                  {(data.matrixRows || []).map((row, index) => (
                    <div key={index} className="sub-row-item">
                      <input
                        type="text"
                        value={getMatrixRowText(row)}
                        placeholder="Texto de la fila"
                        onChange={(event) => handleMatrixRowChange(index, event.target.value)}
                      />
                      <button type="button" aria-label="Eliminar fila" onClick={() => removeMatrixRow(index)}>
                        <MdDeleteOutline aria-hidden="true" />
                      </button>
                    </div>
                  ))}
                  <button type="button" className="btn-add-subitem" onClick={addMatrixRow}>
                    <MdAdd aria-hidden="true" />
                    Añadir fila
                  </button>
                </div>
                <div className="config-help-cell">Elementos que se valorarán con la misma escala.</div>
              </div>
            )}
          </>
        )}

        {CHOICE_QUESTION_TYPES.includes(data.questionType) && (
          <>
            <div className="config-grid-row">
              <div className="config-label-cell">Opciones:</div>
              <div className="config-interactive-cell full-width-field">
                {(data.options || []).map((option, index) => (
                  <div key={index} className="sub-row-item">
                    <input
                      type="text"
                      value={getOptionText(option)}
                      placeholder="Texto de la opción"
                      onChange={(event) => handleOptionChange(index, 'label', event.target.value)}
                    />
                    {data.evaluationDirection !== 'NEUTRAL' && (
                      <input
                        type="number"
                        step="0.1"
                        value={getOptionFactor(option)}
                        placeholder="Factor"
                        className="opt-factor-input"
                        onChange={(event) => handleOptionChange(index, 'evaluationFactor', parseNumberValue(event.target.value))}
                      />
                    )}
                    <button type="button" aria-label="Eliminar opción" onClick={() => removeOption(index)}>
                      <MdDeleteOutline aria-hidden="true" />
                    </button>
                  </div>
                ))}

                <div className="options-actions-bar">
                  <button type="button" className="btn-add-subitem" onClick={addOption}>
                    <MdAdd aria-hidden="true" />
                    Añadir opción
                  </button>

                  <label className="checkbox-sublabel">
                    <input
                      type="checkbox"
                      checked={Boolean(data.hasOtherOption)}
                      onChange={(event) => handleField('hasOtherOption', event.target.checked)}
                    />
                    <span>Permitir opción &quot;Otro&quot;</span>
                  </label>
                </div>
              </div>
              <div className="config-help-cell">Opciones seleccionables por el encuestado.</div>
            </div>

            {data.questionType === 'MULTIPLE_CHOICE' && (() => {
              const totalAvailable = (data.options || []).length + (data.hasOtherOption ? 1 : 0);
              return (
                <div className="config-grid-row">
                  <div className="config-label-cell">Límites de selección:</div>
                  <div className="config-interactive-cell limits-labeled-group">
                    <div className="labeled-input-item">
                      <span className="mini-top-label">Mínimo</span>
                      <input
                        type="number"
                        min="1"
                        max={data.maxSelections || totalAvailable || 1}
                        placeholder="1"
                        value={data.minSelections ?? ''}
                        onChange={(event) => handleField('minSelections', parseIntegerValue(event.target.value))}
                      />
                    </div>
                    <div className="labeled-input-item">
                      <span className="mini-top-label">Máximo</span>
                      <input
                        type="number"
                        min={data.minSelections || 1}
                        max={totalAvailable || 1}
                        placeholder={String(totalAvailable || 1)}
                        value={data.maxSelections ?? ''}
                        onChange={(event) => handleField('maxSelections', parseIntegerValue(event.target.value))}
                      />
                    </div>
                  </div>
                  <div className="config-help-cell">
                    Valores válidos: Mínimo desde 1 hasta el máximo seleccionado. Máximo hasta {totalAvailable} (total de opciones).
                  </div>
                </div>
              );
            })()}
          </>
        )}

        {data.questionType === 'BOOLEAN' && (
          <div className="config-grid-row">
            <div className="config-label-cell">Respuestas:</div>
            <div className="config-interactive-cell full-width-field">
              <div className="range-inputs">
                <input
                  type="text"
                  value={data.trueLabel || ''}
                  placeholder="Etiqueta afirmativa"
                  onChange={(event) => handleField('trueLabel', event.target.value)}
                />
                {data.evaluationDirection !== 'NEUTRAL' && (
                  <input
                    type="number"
                    step="0.1"
                    value={data.trueFactor ?? ''}
                    placeholder="Factor"
                    onChange={(event) => handleField('trueFactor', parseNumberValue(event.target.value))}
                  />
                )}
              </div>
              <div className="range-inputs boolean-answer-row">
                <input
                  type="text"
                  value={data.falseLabel || ''}
                  placeholder="Etiqueta negativa"
                  onChange={(event) => handleField('falseLabel', event.target.value)}
                />
                {data.evaluationDirection !== 'NEUTRAL' && (
                  <input
                    type="number"
                    step="0.1"
                    value={data.falseFactor ?? ''}
                    placeholder="Factor"
                    onChange={(event) => handleField('falseFactor', parseNumberValue(event.target.value))}
                  />
                )}
              </div>
            </div>
            <div className="config-help-cell">Textos y factores asociados a cada respuesta.</div>
          </div>
        )}
      </section>

      <section className="builder-card preview-section">
        <h2 className="builder-section-title">VISTA PREVIA DE LA PREGUNTA</h2>
        <div className="preview-container">
          <Question
            questionNumber="Q"
            data={data}
            isCatalog={isCatalog}
            interactive={false}
          />
        </div>
      </section>
    </div>
  );
}