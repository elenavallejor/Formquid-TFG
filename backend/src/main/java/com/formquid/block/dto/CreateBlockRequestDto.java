package com.formquid.block.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos necesarios para crear un bloque de catálogo.
 */
public record CreateBlockRequestDto(
    @NotBlank String name,
    String description,
    @NotEmpty List<@NotNull @Valid CreateBlockItemRequestDto> items
) {
}