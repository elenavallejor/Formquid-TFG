package com.formquid.participation.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record MatrixAnswerRequestDto(
    @NotNull UUID matrixRowId,
    @NotNull BigDecimal numericValue
) {
}
