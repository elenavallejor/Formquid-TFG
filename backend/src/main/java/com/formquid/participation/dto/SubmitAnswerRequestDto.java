package com.formquid.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record SubmitAnswerRequestDto(
    @NotBlank String questionId,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    List<@Valid OptionAnswerRequestDto> selectedOptions,
    List<@Valid MatrixAnswerRequestDto> matrixValues
) {
}
