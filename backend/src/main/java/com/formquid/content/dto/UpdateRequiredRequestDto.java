package com.formquid.content.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRequiredRequestDto(
    @NotNull Boolean required
) {
}
