package com.formquid.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public record ReorderContentItemRequestDto(
    @NotNull Long itemId,
    @NotNull @Min(1) Integer position
) {
}
