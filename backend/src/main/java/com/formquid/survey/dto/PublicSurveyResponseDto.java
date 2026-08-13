package com.formquid.survey.dto;

import java.util.List;

/**
 * Representación pública completa de una encuesta.
 *
 * Solo contiene los datos necesarios para mostrar y responder
 * el cuestionario.
 */
public record PublicSurveyResponseDto(
    String publicCode,
    String title,
    String description,
    String imageUrl,
    List<String> tagNames,
    List<PublicSurveyBlockDto> blocks
) {}
