package com.formquid.survey.dto;

import com.formquid.common.Category;
import com.formquid.survey.entity.SurveyLifecycle;

import java.time.Instant;

/**
 * Resume una encuesta que ya dispone de participaciones consultables.
 */
public record SurveyResultsSummaryDto(
    Long id,
    String title,
    String imageKey,
    String editorName,
    SurveyLifecycle lifecycleStatus,
    long responses,
    Instant updatedAt,
    Category category
) {
}
