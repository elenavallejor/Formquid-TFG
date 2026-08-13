package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO de entrada para un grupo de preguntas dentro de una plantilla.
 */
public record TemplateBlockRequestDto(

    /**
     * Identificador del bloque reutilizable.
     *
     * Puede ser null si pregunta no pertenece a ningún bloque.
     */
    Long blockId,

    @Valid
    @NotEmpty(message = "Cada grupo de la plantilla debe contener al menos una pregunta")
    List<TemplateQuestionRequestDto> questions
) {}