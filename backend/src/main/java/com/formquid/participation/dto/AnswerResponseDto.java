package com.formquid.participation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnswerResponseDto(
    UUID id,
    String questionId,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    List<UUID> selectedOptionIds,
    List<UUID> matrixRowIds
) {
}
