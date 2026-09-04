import { useMemo } from 'react';
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

const FACE_CONFIG = [
  { icon: BsEmojiAngryFill, color: '#ef4444' },
  { icon: BsEmojiFrownFill, color: '#f97316' },
  { icon: BsEmojiNeutralFill, color: '#eab308' },
  { icon: BsEmojiSmileFill, color: '#84cc16' },
  { icon: BsEmojiLaughingFill, color: '#22c55e' },
];

export function Question({
  questionNumber = 'Q',
  data = {},
  isCatalog = false,
  interactive = false,
  value = null,
  onChange = () => {},
}) {
  const scaleValues = useMemo(() => getScaleValues(data), [data]);
  const statement = data.statement || 'Enunciado de la pregunta...';
  const annotation = data.annotation;
  const displayType = getScaleDisplayValue(data);

  const minVal = scaleValues[0];
  const maxVal = scaleValues[scaleValues.length - 1];

  function renderVisualItem(val, index, isSelected, onClick) {
    if (displayType === 'STARS') {
      const isHalf = Number(val) % 1 !== 0;
      return (
        <span
          className={`scale-star-item ${isSelected ? 'selected' : 'unselected'}`}
          onClick={onClick}
        >
          {isSelected ? (isHalf ? <BsStarHalf /> : <BsStarFill />) : <BsStar />}
        </span>
      );
    }

    if (displayType === 'FACES') {
      const faceCfg = FACE_CONFIG[index] || FACE_CONFIG[2];
      const FaceIcon = faceCfg.icon;

      return (
        <span
          className={`scale-face-item ${isSelected ? 'selected' : 'unselected'}`}
          style={{ color: isSelected ? faceCfg.color : '#cbd5e1' }}
          onClick={onClick}
        >
          <FaceIcon />
        </span>
      );
    }

    // Por defecto: Círculo (radio)
    return (
      <span
        className={`academic-matrix-circle ${isSelected ? 'selected' : ''}`}
        onClick={onClick}
      />
    );
  }

  return (
    <article className="academic-question-card">
      <h3 className="academic-statement">
        {questionNumber && <span className="question-prefix">{questionNumber}.</span>}{' '}
        {statement}
        {!isCatalog && data.isRequired && <span className="required-asterisk"> *</span>}
      </h3>

      {annotation && (
        <div className="academic-annotation-box">
          <span className="annotation-prefix">Nota:</span> {annotation}
        </div>
      )}

      <div className="academic-controls-container">
        {/* 1. TEXT */}
        {data.questionType === 'TEXT' && (
          <div className="academic-text-box">
            <textarea
              className="academic-text-field"
              rows={3}
              maxLength={data.maxLength || undefined}
              placeholder="Escriba aquí su respuesta..."
              disabled={!interactive}
              value={value || ''}
              onChange={(e) => onChange(e.target.value)}
            />
          </div>
        )}

        {/* 2. NUMBER */}
        {data.questionType === 'NUMBER' && (
          <div className="academic-number-wrapper">
            <input
              type="number"
              className="academic-number-input"
              min={data.minValue ?? undefined}
              max={data.maxValue ?? undefined}
              step={data.step ?? 1}
              placeholder="0"
              disabled={!interactive}
              value={value ?? ''}
              onChange={(e) => onChange(e.target.value)}
            />
            {(data.minValue !== undefined || data.maxValue !== undefined) && (
              <span className="academic-number-hint">
                (Entre {data.minValue ?? 0} y {data.maxValue ?? 100})
              </span>
            )}
          </div>
        )}

        {/* 3. SINGLE_CHOICE */}
        {data.questionType === 'SINGLE_CHOICE' && (
          <ul className="academic-options-list">
            {(data.options || []).map((opt, idx) => {
              const optText = getOptionText(opt);
              return (
                <li
                  key={idx}
                  className="academic-option-row"
                  onClick={() => interactive && onChange(optText)}
                >
                  <span className="option-index">{idx}.</span>
                  {interactive ? (
                    <input
                      type="radio"
                      name={`question_${questionNumber}`}
                      value={optText}
                      checked={value === optText}
                      onChange={() => onChange(optText)}
                    />
                  ) : (
                    <span className="academic-radio-circle" />
                  )}
                  <span className="option-label">{optText || `Opción ${idx + 1}`}</span>
                </li>
              );
            })}

            {data.hasOtherOption && (
              <li className="academic-option-row">
                <span className="option-index">{(data.options || []).length}.</span>
                {interactive ? (
                  <input type="radio" name={`question_${questionNumber}`} disabled />
                ) : (
                  <span className="academic-radio-circle" />
                )}
                <span className="option-label">Otro: __________________________</span>
              </li>
            )}
          </ul>
        )}

        {/* 4. MULTIPLE_CHOICE */}
        {data.questionType === 'MULTIPLE_CHOICE' && (
          <ul className="academic-options-list">
            {(data.options || []).map((opt, idx) => {
              const optText = getOptionText(opt);
              const isChecked = Array.isArray(value) && value.includes(optText);
              return (
                <li key={idx} className="academic-option-row">
                  <span className="option-index">{idx}.</span>
                  {interactive ? (
                    <input
                      type="checkbox"
                      value={optText}
                      checked={isChecked}
                      onChange={(e) => {
                        const current = Array.isArray(value) ? [...value] : [];
                        onChange(
                          e.target.checked
                            ? [...current, optText]
                            : current.filter((v) => v !== optText)
                        );
                      }}
                    />
                  ) : (
                    <span className="academic-checkbox-square" />
                  )}
                  <span className="option-label">{optText || `Opción ${idx + 1}`}</span>
                </li>
              );
            })}

            {data.hasOtherOption && (
              <li className="academic-option-row">
                <span className="option-index">{(data.options || []).length}.</span>
                {interactive ? (
                  <input type="checkbox" disabled />
                ) : (
                  <span className="academic-checkbox-square" />
                )}
                <span className="option-label">Otro: __________________________</span>
              </li>
            )}
          </ul>
        )}

        {/* 5. SCALE */}
        {data.questionType === 'SCALE' && (
          <div className="academic-scale-block">
            {data.leftLabel && <span className="scale-bound-label">{data.leftLabel}</span>}
            <div className="scale-columns-row">
              {scaleValues.map((val, idx) => {
                const isSelected = value === val;
                return (
                  <div key={val} className="scale-column-unit">
                    {displayType === 'NUMERIC' && (
                      <span className="scale-unit-number">{val}</span>
                    )}
                    {renderVisualItem(val, idx, isSelected, () => interactive && onChange(val))}
                  </div>
                );
              })}
            </div>
            {data.rightLabel && <span className="scale-bound-label">{data.rightLabel}</span>}
          </div>
        )}

        {/* 6. MATRIX_SCALE */}
        {data.questionType === 'MATRIX_SCALE' && (
          <div className="academic-matrix-wrapper">
            <table className="academic-matrix-table">
              <thead>
                <tr>
                  <th className="matrix-aspect-th">Aspecto</th>
                  {scaleValues.map((val) => {
                    const isMin = val === minVal;
                    const isMax = val === maxVal;
                    let label = `${val}`;
                    if (isMin && data.leftLabel) label = `${val} ${data.leftLabel}`;
                    if (isMax && data.rightLabel) label = `${val} ${data.rightLabel}`;

                    return (
                      <th key={val} className="matrix-scale-th">
                        {label}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody>
                {(data.matrixRows || []).map((row, rIdx) => {
                  const rowLabel = getMatrixRowText(row);
                  const selectedVal = value?.[rIdx];

                  return (
                    <tr key={rIdx} className="matrix-data-row">
                      <td className="matrix-row-title">{rowLabel || `Fila ${rIdx + 1}`}</td>
                      {scaleValues.map((val, idx) => {
                        const isSelected = selectedVal === val;
                        return (
                          <td key={val} className="matrix-cell-radio">
                            {renderVisualItem(val, idx, isSelected, () => {
                              if (interactive) {
                                onChange({ ...value, [rIdx]: val });
                              }
                            })}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* 7. BOOLEAN */}
        {data.questionType === 'BOOLEAN' && (
          <div className="academic-boolean-horizontal">
            {[
              { key: 'true', label: data.trueLabel || 'Sí' },
              { key: 'false', label: data.falseLabel || 'No' },
            ].map((item) => (
              <button
                key={item.key}
                type="button"
                className={`boolean-choice-pill ${interactive && value === item.key ? 'selected' : ''}`}
                disabled={!interactive}
                onClick={() => interactive && onChange(item.key)}
              >
                <span className="academic-checkbox-square" />
                <span className="boolean-pill-text">{item.label}</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </article>
  );
}