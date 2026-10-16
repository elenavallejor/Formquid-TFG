package com.formquid.survey.dto;

import com.formquid.content.dto.UpdateSurveyContentRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Combina los metadatos con la estructura validada durante la importación. */
public record CreateImportedSurveyRequestDto(
    @NotNull @Valid CreateSurveyRequestDto survey,
    @NotNull @Valid UpdateSurveyContentRequestDto content
) {
}
