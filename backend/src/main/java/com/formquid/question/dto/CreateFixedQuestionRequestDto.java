package com.formquid.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFixedQuestionRequestDto(
    @NotBlank String catalogName,
    @Valid @NotNull QuestionDraftDto question
) {
}
