package com.formquid.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SubmitAnswerRequestDto(
    @NotNull Long questionId,
    String textValue,
    BigDecimal numericValue,
    Boolean booleanValue,
    List<@Valid OptionAnswerRequestDto> selectedOptions,
    List<@Valid MatrixAnswerRequestDto> matrixValues
) {
}