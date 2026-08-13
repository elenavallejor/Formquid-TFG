package com.formquid.fixedquestion.dto;

import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.FixedQuestionStatus;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de una pregunta fija devueltos por la API.
 */
public record FixedQuestionResponseDto(
    Long id,
    String text,
    String description,
    QuestionType type,
    EvaluationDirection evaluationDirection,
    FixedQuestionStatus status,
    Long blockId,
    String blockName,
    Long createdByUserId,
    boolean requiredByDefault,
    Integer minValue,
    Integer maxValue,
    ScaleDisplayType scaleDisplayType,
    String leftLabel,
    String rightLabel,
    List<OptionConfigResponseDto> options,
    List<MatrixRowResponseDto> matrixRows,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}