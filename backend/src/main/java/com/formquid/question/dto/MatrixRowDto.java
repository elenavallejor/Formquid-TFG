package com.formquid.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record MatrixRowDto(
    Long id,
    @NotBlank String label,
    @NotNull @Min(1) Integer position
) {
}