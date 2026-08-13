package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para actualizar una plantilla reutilizable.
 *
 * Si blocks es null, solo se actualizan los datos generales.
 * Si blocks viene informado, se reemplaza la estructura de bloques y preguntas.
 */
public record UpdateSurveyTemplateRequestDto(

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    String name,

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    String description,

    @Valid
    List<TemplateBlockRequestDto> blocks
) {}