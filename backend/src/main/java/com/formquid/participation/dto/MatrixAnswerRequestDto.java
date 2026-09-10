package com.formquid.participation.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MatrixAnswerRequestDto(
    @NotNull Long matrixRowId,
    @NotNull BigDecimal numericValue
) {
}