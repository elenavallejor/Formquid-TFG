package com.formquid.survey.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para acceder a una encuesta pública o privada.
 *
 * La contraseña solo es necesaria cuando la encuesta tiene estado PRIVATE.
 * En encuestas PUBLIC puede omitirse.
 */
public record AccessSurveyRequestDto(

    @Size(max = 255, message = "La contraseña de acceso no puede superar los 255 caracteres")
    String accessPassword
) {}
