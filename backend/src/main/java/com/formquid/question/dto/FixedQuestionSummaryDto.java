package com.formquid.question.dto;

import com.formquid.question.entity.QuestionType;

import java.time.Instant;

/**
 * Contiene los datos necesarios para listar una pregunta fija del catálogo.
 */
public record FixedQuestionSummaryDto(
    Long id,
    String statement,
    QuestionType questionType,
    String creatorName,
    Instant createdAt,
    Instant updatedAt
) {
}
