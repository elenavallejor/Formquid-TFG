package com.formquid.question.dto;

import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contiene la definición completa de una pregunta.
 */
public record QuestionResponseDto(
    String id,
    QuestionScope scope,
    String statement,
    String annotation,
    QuestionType questionType,
    EvaluationDirection evaluationDirection,
    BigDecimal evaluationWeight,
    QuestionSettingsDto settings,
    List<QuestionOptionDto> options,
    List<MatrixRowDto> matrixRows,
    String sourceFixedQuestionId
) {
}