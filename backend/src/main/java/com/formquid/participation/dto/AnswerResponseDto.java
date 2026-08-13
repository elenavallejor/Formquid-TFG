package com.formquid.participation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida para una respuesta individual.
 */
public record AnswerResponseDto(
    Long id,
    Long surveyQuestionId,
    String questionText,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    List<String> selectedValues,
    List<MatrixAnswerResponseDto> matrixValues,
    String freeTextValue,
    BigDecimal scoreValue
) {}