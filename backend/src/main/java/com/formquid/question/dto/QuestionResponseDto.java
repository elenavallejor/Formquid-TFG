package com.formquid.question.dto;

import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Contiene la definición completa de una pregunta.
 */
public record QuestionResponseDto(
    Long id,
    QuestionScope scope,
    String statement,
    String annotation,
    QuestionType questionType,
    EvaluationDirection evaluationDirection,
    BigDecimal evaluationWeight,
    QuestionSettingsDto settings,
    List<QuestionOptionDto> options,
    List<MatrixRowDto> matrixRows,
    Long sourceFixedQuestionId,
    Long creatorId,
    String creatorName,
    Instant createdAt,
    Instant updatedAt,
    Long usageCount
) {
}
