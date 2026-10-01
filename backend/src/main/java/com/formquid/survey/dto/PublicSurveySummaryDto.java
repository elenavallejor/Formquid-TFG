package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyAccessMode;

import java.util.List;

/**
 * Representa los datos mínimos de una encuesta mostrada en listados públicos.
 */
public record PublicSurveySummaryDto(
    String publicCode,
    String title,
    String description,
    String imageKey,
    SurveyAccessMode accessMode,
    List<String> tags
) {
}
