package com.formquid.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitParticipationRequestDto(
    String accessPassword,
    @NotEmpty List<@Valid SubmitAnswerRequestDto> answers
) {
}
