package com.formquid.auth.dto;

import com.formquid.user.entity.UserRole;

import java.util.UUID;

/**
 * Respuesta devuelta tras un inicio de sesión correcto.
 */
public record AuthUserResponseDto(
    UUID id,
    String email,
    String name,
    UserRole role
) {
}
