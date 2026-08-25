package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyLifecycle;

import java.time.Instant;
import java.util.List;

/**
 * Representa los datos mínimos necesarios para mostrar una encuesta en listados privados.
 */
public record SurveySummaryDto(
    String id,
    String title,
    String imageKey,
    SurveyLifecycle lifecycleStatus,
    List<String> tags,
    Instant updatedAt
) {
}
