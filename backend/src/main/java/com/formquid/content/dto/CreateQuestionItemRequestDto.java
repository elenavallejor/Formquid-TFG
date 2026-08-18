package com.formquid.content.dto;

import com.formquid.question.dto.QuestionDraftDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionItemRequestDto(
    @NotNull @Min(1) Integer position,
    @NotNull Boolean required,
    @Valid @NotNull QuestionDraftDto question
) {
}
