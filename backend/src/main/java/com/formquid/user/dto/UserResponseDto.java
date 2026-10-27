package com.formquid.user.dto;

import com.formquid.user.entity.UserRole;

import java.time.Instant;

/**
 * Representa los datos públicos y administrativos de un usuario registrado.
 */
public record UserResponseDto(
    Long id,
    UserRole role,
    String email,
    String name,
    String institution,
    String country,
    String profileImageKey,
    Instant createdAt,
    Instant updatedAt
) {
}
