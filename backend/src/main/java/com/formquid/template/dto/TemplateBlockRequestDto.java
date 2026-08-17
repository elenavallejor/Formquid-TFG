package com.formquid.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para un grupo de preguntas dentro de una plantilla.
 */
public record TemplateBlockRequestDto(

    @Size(max = 80, message = "La clave del bloque no puede superar los 80 caracteres")
    String blockKey,

    /**
     * Identificador del bloque reutilizable.
     *
     * Puede ser null si el grupo no procede del catálogo.
     */
    Long blockId,

    @Valid
    @NotEmpty(message = "Cada grupo de la plantilla debe contener al menos una pregunta")
    List<TemplateQuestionRequestDto> questions
) {}
