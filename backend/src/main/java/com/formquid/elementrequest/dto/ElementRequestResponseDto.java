package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.RequestedElementType;

import java.time.Instant;
import java.util.Map;

public record ElementRequestResponseDto(
    Long id,
    Long requestedByUserId,
    RequestedElementType elementType,
    String message,
    Map<String, Object> requestPayload,
    Instant createdAt
) {
}
