package com.formquid.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderContentRequestDto(
    @NotEmpty List<@Valid ReorderContentItemRequestDto> items
) {
}
