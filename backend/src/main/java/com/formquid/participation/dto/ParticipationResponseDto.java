package com.formquid.participation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParticipationResponseDto(
    UUID id,
    String surveyId,
    Instant submittedAt,
    List<AnswerResponseDto> answers
) {
}
