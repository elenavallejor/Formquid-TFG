package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTemplateRequestDto(
    @NotBlank String title,
    String description,
    String imageKey,
    @NotNull List<@NotNull @Valid CreateTemplateItemRequestDto> items
) {
}
