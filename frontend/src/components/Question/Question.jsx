import { useId, useMemo } from 'react';
import { BsStar, BsStarFill, BsStarHalf } from 'react-icons/bs';
import {
  BsEmojiAngryFill,
  BsEmojiFrownFill,
  BsEmojiNeutralFill,
  BsEmojiSmileFill,
  BsEmojiLaughingFill,
} from 'react-icons/bs';

import {
  getMatrixRowText,
  getOptionText,
  getScaleDisplayValue,
  getScaleValues,
} from '../../utils/questionBuilderUtils.js';
import './Question.css';

const FACE_ICONS = [BsEmojiAngryFill, BsEmojiFrownFill, BsEmojiNeutralFill, BsEmojiSmileFill, BsEmojiLaughingFill];

function isOtherOption(option) {
  return Boolean(option?.other ?? option?.isOther);
}

function isOtherAnswer(answer) {
  return Boolean(answer && typeof answer === 'object' && answer.isOther);
}

function joinIds(...ids) {
  const value = ids.filter(Boolean).join(' ');
  return value || undefined;
}

export function Question({
  questionNumber = 'Q',
  headingLevel = 3,
  data = {},
  isCatalog = false,
  interactive = false,
  value = null,
  onChange = () => {},
  idBase,
  errorMessage = '',
}) {
  const normalizedHeadingLevel = Math.min(6, Math.max(2, Number(headingLevel) || 3));
  const Heading = `h${normalizedHeadingLevel}`;
  const generatedId = useId().replaceAll(':', '');
  const questionId = idBase || `question-${generatedId}`;
  const titleId = `${questionId}-title`;
  const annotationId = `${questionId}-annotation`;
  const hintId = `${questionId}-hint`;
  const selectionHintId = `${questionId}-selection-hint`;
  const errorId = `${questionId}-error`;
  const groupId = `${questionId}-group`;
  const controlId = `${questionId}-control`;
  const isRequired = !isCatalog && Boolean(data.isRequired);
  const scaleValues = useMemo(() => getScaleValues(data), [data]);
  const statement = data.statement || 'Enunciado de la pregunta...';
  const annotation = data.annotation;
  const describedBy = joinIds(annotation && annotationId, errorMessage && errorId);
  const displayType = getScaleDisplayValue(data);
  const minVal = scaleValues[0];
  const maxVal = scaleValues[scaleValues.length - 1];
  const options = useMemo(() => {
    const definedOptions = Array.isArray(data.options) ? [...data.options] : [];
    if (data.hasOtherOption && !definedOptions.some(isOtherOption)) {
      definedOptions.push({ label: 'Otro', other: true });
    }
    return definedOptions;
  }, [data.hasOtherOption, data.options]);

  function renderVisualItem(val, index, isSelected) {
    if (displayType === 'STARS') {
      const isHalf = Number(val) % 1 !== 0;
      return (
        <span aria-hidden="true" className="scale-star-item">
          {isSelected ? (isHalf ? <BsStarHalf /> : <BsStarFill />) : <BsStar />}
        </span>
      );
    }

    const FaceIcon = FACE_ICONS[index] || FACE_ICONS[2];
    return (
      <span aria-hidden="true" className={`scale-face-item ${isSelected ? 'selected' : 'unselected'}`}>
        <FaceIcon />
      </span>
    );
  }

  function renderGroup(children, additionalDescriptionId) {
    return (
      <fieldset
        id={groupId}
        className="academic-question-group"
        aria-describedby={joinIds(describedBy, additionalDescriptionId)}
        aria-invalid={errorMessage ? 'true' : undefined}
        tabIndex={errorMessage ? -1 : undefined}
      >
        <legend className="sr-only">
          {questionNumber ? `${questionNumber}. ` : ''}{statement}{isRequired ? ' (obligatoria)' : ''}
        </legend>
        {children}
      </fieldset>
    );
  }

  function renderChoices(multiple = false) {
    const selectedValues = multiple && Array.isArray(value) ? value : [];
    const otherAnswer = multiple
      ? selectedValues.find(isOtherAnswer) || null
      : isOtherAnswer(value) ? value : null;
    const minimum = data.minSelections === '' || data.minSelections == null ? null : Number(data.minSelections);
    const maximum = data.maxSelections === '' || data.maxSelections == null ? null : Number(data.maxSelections);
    const selectionHint = multiple && Number.isFinite(minimum) && Number.isFinite(maximum)
      ? `Selecciona entre ${minimum} y ${maximum} opciones.`
      : multiple && Number.isFinite(maximum)
        ? `Selecciona como máximo ${maximum} opciones.`
        : multiple && Number.isFinite(minimum)
          ? `Selecciona al menos ${minimum} opciones.`
          : '';

    function buildOtherAnswer(option, otherText = '') {
      return { isOther: true, optionId: option.id ?? null, otherText };
    }

    function updateSelection(option, optionText, other, checked) {
      if (!multiple) {
        if (checked) onChange(other ? buildOtherAnswer(option, otherAnswer?.otherText) : optionText);
        return;
      }

      if (other) {
        const withoutOther = selectedValues.filter((entry) => !isOtherAnswer(entry));
        onChange(checked
          ? [...withoutOther, buildOtherAnswer(option, otherAnswer?.otherText)]
          : withoutOther);
        return;
      }

      onChange(checked
        ? [...selectedValues, optionText]
        : selectedValues.filter((entry) => entry !== optionText));
    }

    function updateOtherText(option, otherText) {
      const answer = buildOtherAnswer(option, otherText);
      if (!multiple) {
        onChange(answer);
        return;
      }
      onChange([...selectedValues.filter((entry) => !isOtherAnswer(entry)), answer]);
    }

    return renderGroup(
      <>
        {selectionHint && <p id={selectionHintId} className="academic-selection-hint">{selectionHint}</p>}
        <ul className="academic-options-list">
          {options.map((option, index) => {
            const optionText = getOptionText(option) || `Opción ${index + 1}`;
            const optionId = `${questionId}-option-${index}`;
            const other = isOtherOption(option);
            const checked = other
              ? Boolean(otherAnswer)
              : multiple ? selectedValues.includes(optionText) : value === optionText;

            return (
              <li key={option.id ?? `${optionText}-${index}`} className="academic-option-row">
                <span aria-hidden="true" className="option-index">{index + 1}.</span>
                <div className="academic-option-content">
                  <label className="academic-option-label" htmlFor={optionId}>
                    <input
                      id={optionId}
                      type={multiple ? 'checkbox' : 'radio'}
                      name={multiple ? undefined : `${questionId}-single`}
                      value={other ? '__other__' : optionText}
                      checked={checked}
                      disabled={!interactive}
                      required={!multiple && interactive && isRequired}
                      aria-invalid={errorMessage ? 'true' : undefined}
                      onChange={(event) => updateSelection(option, optionText, other, event.target.checked)}
                    />
                    <span className="option-label">{optionText}</span>
                  </label>
                  {other && (
                    <label className="academic-other-field">
                      <span className="sr-only">Especifica la respuesta «Otro»</span>
                      <input
                        type="text"
                        maxLength={500}
                        placeholder="Especifica tu respuesta"
                        disabled={!interactive}
                        required={interactive && checked}
                        aria-describedby={describedBy}
                        aria-invalid={errorMessage && checked ? 'true' : undefined}
                        value={otherAnswer?.otherText || ''}
                        onFocus={() => {
                          if (!checked) updateSelection(option, optionText, true, true);
                        }}
                        onChange={(event) => updateOtherText(option, event.target.value)}
                      />
                    </label>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      </>,
      selectionHint ? selectionHintId : undefined,
    );
  }

  return (
    <article className="academic-question-card" aria-labelledby={titleId}>
      <Heading id={titleId} className="academic-statement">
        {questionNumber && <span className="question-prefix">{questionNumber}.</span>}{' '}
        {statement}
        {isRequired && (
          <>
            <span aria-hidden="true" className="required-asterisk"> *</span>
            <span className="sr-only"> (obligatoria)</span>
          </>
        )}
      </Heading>

      {annotation && (
        <div id={annotationId} className="academic-annotation-box">
          <span className="annotation-prefix">Nota:</span> {annotation}
        </div>
      )}

      <div className="academic-controls-container">
        {/* 1. TEXT */}
        {data.questionType === 'TEXT' && (
          <div className="academic-text-box">
            <label className="sr-only" htmlFor={controlId}>{statement}</label>
            <textarea
              id={controlId}
              className="academic-text-field"
              rows={3}
              maxLength={data.maxLength || undefined}
              placeholder="Escriba aquí su respuesta..."
              disabled={!interactive}
              required={interactive && isRequired}
              aria-describedby={describedBy}
              aria-invalid={errorMessage ? 'true' : undefined}
              value={value || ''}
              onChange={(e) => onChange(e.target.value)}
            />
          </div>
        )}

        {/* 2. NUMBER */}
        {data.questionType === 'NUMBER' && (
          <div className="academic-number-wrapper">
            <label className="sr-only" htmlFor={controlId}>{statement}</label>
            <input
              id={controlId}
              type="number"
              className="academic-number-input"
              min={data.minValue ?? undefined}
              max={data.maxValue ?? undefined}
              step={data.step ?? 1}
              placeholder="0"
              disabled={!interactive}
              required={interactive && isRequired}
              aria-describedby={joinIds(describedBy, hintId)}
              aria-invalid={errorMessage ? 'true' : undefined}
              value={value ?? ''}
              onChange={(e) => onChange(e.target.value)}
            />
            {(data.minValue !== undefined || data.maxValue !== undefined) && (
              <span id={hintId} className="academic-number-hint">
                (Entre {data.minValue ?? 0} y {data.maxValue ?? 100})
              </span>
            )}
          </div>
        )}

        {/* 3. SINGLE_CHOICE */}
        {data.questionType === 'SINGLE_CHOICE' && renderChoices()}

        {/* 4. MULTIPLE_CHOICE */}
        {data.questionType === 'MULTIPLE_CHOICE' && renderChoices(true)}

        {/* 5. SCALE */}
        {data.questionType === 'SCALE' && renderGroup(
          <div className="academic-scale-block">
            {data.leftLabel && <span className="scale-bound-label">{data.leftLabel}</span>}
            <div className="scale-columns-row">
              {scaleValues.map((val, idx) => {
                const isSelected = value === val;
                return (
                  <label
                    key={val}
                    className={`scale-column-unit ${displayType !== 'NUMERIC' ? 'scale-choice-label' : ''}`}
                  >
                    {displayType === 'NUMERIC' && <span className="scale-unit-number">{val}</span>}
                    <input
                      className={displayType === 'NUMERIC' ? '' : 'visual-choice-input'}
                      type="radio"
                      name={`${questionId}-scale`}
                      value={val}
                      checked={isSelected}
                      disabled={!interactive}
                      required={interactive && isRequired}
                      aria-invalid={errorMessage ? 'true' : undefined}
                      onChange={() => onChange(val)}
                    />
                    {displayType !== 'NUMERIC' && (
                      <span className="visual-choice-content">
                        {renderVisualItem(val, idx, isSelected)}
                        <span className="sr-only">Valor {val}</span>
                      </span>
                    )}
                  </label>
                );
              })}
            </div>
            {data.rightLabel && <span className="scale-bound-label">{data.rightLabel}</span>}
          </div>,
        )}

        {/* 6. MATRIX_SCALE */}
        {data.questionType === 'MATRIX_SCALE' && renderGroup(
          <div className="academic-matrix-wrapper">
            <table className="academic-matrix-table">
              <thead>
                <tr>
                  <th className="matrix-aspect-th" scope="col">Aspecto</th>
                  {scaleValues.map((val) => {
                    const isMin = val === minVal;
                    const isMax = val === maxVal;
                    let label = `${val}`;
                    if (isMin && data.leftLabel) label = `${val} ${data.leftLabel}`;
                    if (isMax && data.rightLabel) label = `${val} ${data.rightLabel}`;

                    return (
                      <th id={`${questionId}-column-${val}`} key={val} className="matrix-scale-th" scope="col">
                        {label}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody>
                {(data.matrixRows || []).map((row, rIdx) => {
                  const rowLabel = getMatrixRowText(row) || `Fila ${rIdx + 1}`;
                  const selectedVal = value?.[rIdx];
                  const rowHeaderId = `${questionId}-row-${rIdx}`;

                  return (
                    <tr key={row.id ?? `${rowLabel}-${rIdx}`} className="matrix-data-row">
                      <th id={rowHeaderId} className="matrix-row-title" scope="row">{rowLabel}</th>
                      {scaleValues.map((val, idx) => {
                        const isSelected = selectedVal === val;
                        return (
                          <td key={val} className="matrix-cell-radio">
                            <label className={displayType === 'NUMERIC' ? 'matrix-numeric-label' : 'matrix-scale-choice-label'}>
                              <input
                                className={displayType === 'NUMERIC' ? '' : 'visual-choice-input'}
                                type="radio"
                                name={`${questionId}-matrix-${rIdx}`}
                                value={val}
                                checked={isSelected}
                                disabled={!interactive}
                                required={interactive && isRequired}
                                aria-labelledby={`${rowHeaderId} ${questionId}-column-${val}`}
                                aria-invalid={errorMessage ? 'true' : undefined}
                                onChange={() => onChange({ ...(value || {}), [rIdx]: val })}
                              />
                              {displayType !== 'NUMERIC' && (
                                <span className="visual-choice-content">
                                  {renderVisualItem(val, idx, isSelected)}
                                </span>
                              )}
                            </label>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>,
        )}

        {/* 7. BOOLEAN */}
        {data.questionType === 'BOOLEAN' && renderGroup(
          <div className="academic-boolean-horizontal">
            {[
              { key: 'true', label: data.trueLabel || 'Sí' },
              { key: 'false', label: data.falseLabel || 'No' },
            ].map((item) => {
              const isSelected = value === item.key;
              return (
                <label key={item.key} className="boolean-choice-pill">
                  <input
                    type="radio"
                    name={`${questionId}-boolean`}
                    value={item.key}
                    checked={isSelected}
                    disabled={!interactive}
                    required={interactive && isRequired}
                    aria-invalid={errorMessage ? 'true' : undefined}
                    onChange={() => onChange(item.key)}
                  />
                  <span className="boolean-pill-text">{item.label}</span>
                </label>
              );
            })}
          </div>,
        )}
      </div>

      {errorMessage && <p id={errorId} className="question-error-message">{errorMessage}</p>}
    </article>
  );
}
