package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.RequestedElementType;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;

import java.time.Instant;
import java.util.Map;

public record ElementRequestResponseDto(
    Long id,
    Long requestedByUserId,
    String requestedByName,
    String requestedByEmail,
    String requestedByInstitution,
    UserGender requestedByGender,
    UserRole requestedByRole,
    RequestedElementType elementType,
    String message,
    Map<String, Object> requestPayload,
    Instant createdAt
) {
}
