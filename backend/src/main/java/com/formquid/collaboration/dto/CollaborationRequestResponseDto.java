package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;

import java.time.Instant;

public record CollaborationRequestResponseDto(
    Long id,
    Long surveyId,
    Long userId,
    CollaborationRequestType requestType,
    String message,
    Instant createdAt
) {
}