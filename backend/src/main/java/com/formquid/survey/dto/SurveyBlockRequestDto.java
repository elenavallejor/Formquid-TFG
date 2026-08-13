package com.formquid.survey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para un bloque o grupo de preguntas dentro de una encuesta.
 */
public record SurveyBlockRequestDto(

    /**
     * Identificador del bloque reutilizable de origen.
     *
     * Puede ser null cuando el grupo no proceda de un bloque fijo.
     */
    Long sourceBlockId,

    @Size(max = 180, message = "El título del bloque no puede superar los 180 caracteres")
    String title,

    @Valid
    @NotEmpty(message = "Cada bloque debe contener al menos una pregunta")
    List<SurveyQuestionRequestDto> questions
) {}