package com.formquid.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MatrixRowDto(
    UUID id,
    @NotBlank String label,
    @NotNull @Min(1) Integer position
) {
}
