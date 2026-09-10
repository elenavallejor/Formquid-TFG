package com.formquid.collaboration.dto;

import java.time.Instant;

public record SurveyCollaboratorResponseDto(
    Long surveyId,
    Long userId,
    String email,
    String name,
    Instant joinedAt
) {
}