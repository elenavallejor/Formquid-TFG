package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateSurveyRequestDto(
    @NotBlank @Size(max = 200) String title,
    String description,
    String imageKey,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    String accessPassword,
    @Size(max = 4) List<String> tags
) {
}
