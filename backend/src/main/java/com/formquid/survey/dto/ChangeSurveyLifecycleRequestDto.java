package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyLifecycle;
import jakarta.validation.constraints.NotNull;

/**
 * Recibe el nuevo estado del ciclo de vida solicitado para una encuesta.
 */
public record ChangeSurveyLifecycleRequestDto(
    @NotNull SurveyLifecycle lifecycleStatus
) {
}
