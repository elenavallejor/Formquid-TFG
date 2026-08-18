package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyAccessMode;

public record PublicSurveySummaryDto(
    String id,
    String title,
    String description,
    String imageKey,
    SurveyAccessMode accessMode
) {
}
