package com.formquid.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record QuestionOptionDto(
    Long id,
    @NotBlank String label,
    @NotNull @Min(1) Integer position,
    boolean other,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal evaluationFactor
) {
}
