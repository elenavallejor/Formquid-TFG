package com.formquid.survey.dto;

import com.formquid.content.dto.UpdateSurveyContentRequestDto;

/** Resume el contenido válido detectado en un documento ODT. */
public record SurveyImportPreviewDto(
    String title,
    String description,
    UpdateSurveyContentRequestDto content,
    int blockCount,
    int questionCount
) {
}
