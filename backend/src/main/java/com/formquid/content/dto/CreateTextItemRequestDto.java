package com.formquid.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTextItemRequestDto(
    @NotNull @Min(1) Integer position,
    @NotBlank String textBody
) {
}
