package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.RequestedElementType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ElementRequestResponseDto(
    UUID id,
    UUID requestedByUserId,
    RequestedElementType elementType,
    String message,
    Map<String, Object> requestPayload,
    Instant createdAt
) {
}
