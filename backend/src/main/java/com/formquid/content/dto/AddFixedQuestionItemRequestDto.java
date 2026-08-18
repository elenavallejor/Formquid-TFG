package com.formquid.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddFixedQuestionItemRequestDto(
    @NotNull @Min(1) Integer position,
    @NotNull Boolean required,
    @NotNull String fixedQuestionId
) {
}
