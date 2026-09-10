package com.formquid.participation.dto;

import java.time.Instant;
import java.util.List;

public record ParticipationResponseDto(
    Long id,
    String publicCode,
    Instant submittedAt,
    List<AnswerResponseDto> answers
) {
}