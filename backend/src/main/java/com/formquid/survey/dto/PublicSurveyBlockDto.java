package com.formquid.survey.dto;

import java.util.List;

/**
 * Representación pública de un bloque de encuesta.
 *
 * No expone identificadores del catálogo interno.
 */
public record PublicSurveyBlockDto(
    String title,
    Integer position,
    List<PublicSurveyQuestionDto> questions
) {}
