package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para crear una plantilla reutilizable.
 */
public record CreateSurveyTemplateRequestDto(

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    String name,

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    String description,

    @Valid
    @NotEmpty(message = "La plantilla debe contener al menos un bloque")
    List<TemplateBlockRequestDto> blocks
) {}
