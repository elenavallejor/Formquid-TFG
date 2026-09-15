package com.formquid.question.dto;

import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record QuestionDraftDto(
    @NotBlank String statement,
    String annotation,
    @NotNull QuestionType questionType,
    @NotNull EvaluationDirection evaluationDirection,
    BigDecimal evaluationWeight,
    @Valid QuestionSettingsDto settings,
    @Size(max = 50) List<@NotNull @Valid QuestionOptionDto> options,
    @Size(max = 50) List<@NotNull @Valid MatrixRowDto> matrixRows
) {
}
