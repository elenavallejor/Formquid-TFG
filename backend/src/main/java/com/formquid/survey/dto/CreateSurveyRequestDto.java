package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateSurveyRequestDto(
    @NotBlank @Size(max = 200) String title,
    String description,
    String imageKey,
    UUID sourceTemplateId,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    String accessPassword,
    @Size(max = 4) List<String> tags
) {
}
