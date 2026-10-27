package com.formquid.auth.dto;

import com.formquid.user.entity.UserRole;

/**
 * Respuesta devuelta tras un inicio de sesión correcto.
 */
public record AuthUserResponseDto(
    Long id,
    String email,
    String name,
    UserRole role,
    String profileImageKey
) {
}
