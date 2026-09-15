package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTemplateRequestDto(
    @NotBlank @Size(max = 200) String title,
    String description,
    String imageKey,
    @NotEmpty List<@NotNull @Valid CreateTemplateItemRequestDto> items
) {
}
