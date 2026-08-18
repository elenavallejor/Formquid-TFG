package com.formquid.user.dto;

import com.formquid.user.entity.UserRole;
import com.formquid.user.entity.UserGender;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
    UUID id,
    UserRole role,
    String email,
    String name,
    UserGender gender,
    String institution,
    Instant createdAt,
    Instant updatedAt
) {
}
