package com.formquid.participation.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnswerResponseDto(
    Long id,
    Long questionId,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    List<Long> selectedOptionIds,
    List<Long> matrixRowIds
) {
}