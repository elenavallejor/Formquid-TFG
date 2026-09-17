package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * Recibe el cambio de visibilidad aplicado durante la supervisión administrativa.
 */
public record ChangeSurveyVisibilityRequestDto(
    @NotNull SurveyVisibility visibility
) {
}
