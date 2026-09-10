package com.formquid.participation.dto;

import jakarta.validation.constraints.NotNull;

public record OptionAnswerRequestDto(
    @NotNull Long optionId,
    String otherText
) {
}