package com.formquid.participation.dto;

import java.math.BigDecimal;

public record MatrixAnswerValueResponseDto(
    Long rowId,
    String label,
    BigDecimal numericValue
) {
}
