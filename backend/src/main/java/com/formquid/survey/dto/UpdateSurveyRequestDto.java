package com.formquid.survey.dto;

import com.formquid.common.Category;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyFontFamily;
import com.formquid.survey.entity.SurveyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.formquid.survey.entity.Survey.MAX_TAGS;

/**
 * Recibe los metadatos editables de una encuesta existente.
 */
public record UpdateSurveyRequestDto(
    @NotBlank @Size(max = 200) String title,
    String description,
    String imageKey,
    SurveyFontFamily fontFamily,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    String accessPassword,
    @Size(max = MAX_TAGS) List<String> tags,
    Category category
) {
}
