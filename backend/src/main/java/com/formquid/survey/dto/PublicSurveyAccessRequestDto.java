package com.formquid.survey.dto;

import jakarta.validation.constraints.NotBlank;

/** Recibe la contraseña necesaria para consultar una encuesta protegida. */
public record PublicSurveyAccessRequestDto(
    @NotBlank String accessPassword
) {
}
