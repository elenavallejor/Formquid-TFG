package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyLifecycle;
import jakarta.validation.constraints.NotNull;

public record ChangeSurveyLifecycleRequestDto(
    @NotNull SurveyLifecycle lifecycleStatus
) {
}
