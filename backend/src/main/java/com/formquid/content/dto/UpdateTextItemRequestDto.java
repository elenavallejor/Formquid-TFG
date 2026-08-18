package com.formquid.content.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextItemRequestDto(
    @NotBlank String textBody
) {
}
