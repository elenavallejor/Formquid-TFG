package com.formquid.participation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OptionAnswerRequestDto(
    @NotNull UUID optionId,
    String otherText
) {
}
