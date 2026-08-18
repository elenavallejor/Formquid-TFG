package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;

import java.time.Instant;
import java.util.UUID;

public record CollaborationRequestResponseDto(
    UUID id,
    String surveyId,
    UUID userId,
    CollaborationRequestType requestType,
    String message,
    Instant createdAt
) {
}
