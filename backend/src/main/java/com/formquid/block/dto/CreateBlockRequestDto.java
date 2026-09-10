package com.formquid.block.dto;

import com.formquid.content.dto.CreateQuestionItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Datos necesarios para crear un bloque de catálogo.
 */
public record CreateBlockRequestDto(
    @NotBlank String name,
    String description,
    @NotEmpty List<@Valid CreateQuestionItemRequestDto> questions
) {
}