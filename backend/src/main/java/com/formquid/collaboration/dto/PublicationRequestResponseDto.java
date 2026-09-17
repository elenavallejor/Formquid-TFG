package com.formquid.collaboration.dto;

import java.time.Instant;

public record PublicationRequestResponseDto(
    Long id,
    Long surveyId,
    Long requestedByUserId,
    String requestedByName,
    String requestedByEmail,
    String message,
    Instant createdAt
) {
}
