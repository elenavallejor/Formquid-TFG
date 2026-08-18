package com.formquid.collaboration.dto;

import java.time.Instant;
import java.util.UUID;

public record SurveyCollaboratorResponseDto(
    String surveyId,
    UUID userId,
    String email,
    String name,
    Instant joinedAt
) {
}
