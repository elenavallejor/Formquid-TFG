package com.formquid.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateFixedQuestionRequestDto(
    @Valid @NotNull QuestionDraftDto question
) {
}