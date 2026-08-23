package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;

import java.time.Instant;

public record AdminSurveySummaryDto(
    String id,
    String title,
    String editorName,
    SurveyLifecycle lifecycleStatus,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    Long responses,
    Instant updatedAt
) {
}