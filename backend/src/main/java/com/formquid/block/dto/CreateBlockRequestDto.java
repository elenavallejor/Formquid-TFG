package com.formquid.block.dto;

import com.formquid.common.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos necesarios para crear un bloque de catálogo.
 */
public record CreateBlockRequestDto(
    @NotBlank @Size(max = 150) String name,
    String description,
    @NotEmpty List<@NotNull @Valid CreateBlockItemRequestDto> items,
    Category category
) {
}
