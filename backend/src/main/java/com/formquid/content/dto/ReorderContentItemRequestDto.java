package com.formquid.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReorderContentItemRequestDto(
    @NotNull UUID itemId,
    @NotNull @Min(1) Integer position
) {
}
