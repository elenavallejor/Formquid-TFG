package com.formquid.survey.dto;

import com.formquid.common.Category;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;

import java.time.Instant;

/**
 * Representa la información resumida necesaria para supervisar una encuesta desde administración.
 */
public record AdminSurveySummaryDto(
    Long id,
    String title,
    String imageKey,
    String editorName,
    SurveyLifecycle lifecycleStatus,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    Long responses,
    Instant updatedAt,
    Category category
) {
}
