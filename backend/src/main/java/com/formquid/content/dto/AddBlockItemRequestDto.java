package com.formquid.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddBlockItemRequestDto(
    @NotNull @Min(1) Integer position,
    @NotNull Long blockId
) {
}
