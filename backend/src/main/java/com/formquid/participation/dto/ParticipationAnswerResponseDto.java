package com.formquid.participation.dto;

import com.formquid.question.entity.QuestionType;

import java.math.BigDecimal;
import java.util.List;

public record ParticipationAnswerResponseDto(
    Long answerId,
    Long questionId,
    String statement,
    QuestionType questionType,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    String trueLabel,
    String falseLabel,
    List<SelectedOptionResponseDto> selectedOptions,
    List<MatrixAnswerValueResponseDto> matrixValues
) {
}
