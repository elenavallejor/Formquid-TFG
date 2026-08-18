package com.formquid.content.dto;

import com.formquid.question.dto.QuestionDraftDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionItemRequestDto(
    @NotNull Boolean required,
    @Valid @NotNull QuestionDraftDto question
) {
}
