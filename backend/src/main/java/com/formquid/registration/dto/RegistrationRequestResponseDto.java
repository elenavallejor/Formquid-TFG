package com.formquid.registration.dto;

import com.formquid.common.entity.RequestStatus;
import com.formquid.user.entity.UserGender;

import java.time.Instant;
import java.util.UUID;

public record RegistrationRequestResponseDto(
    UUID id,
    String email,
    String name,
    UserGender gender,
    String institution,
    RequestStatus status,
    Instant createdAt,
    Instant resolvedAt
) {
}
