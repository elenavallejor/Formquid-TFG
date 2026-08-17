package com.formquid.question.dto;

import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionStatus;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de una pregunta devueltos por la API.
 */
public record QuestionResponseDto(
    Long id,
    String text,
    String description,
    QuestionType type,
    EvaluationDirection evaluationDirection,
    QuestionStatus status,
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
