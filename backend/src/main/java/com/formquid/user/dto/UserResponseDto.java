package com.formquid.user.dto;

import com.formquid.user.entity.UserGender;
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
    UserGender gender,
    String institution,
    Instant createdAt,
    Instant updatedAt
) {
}