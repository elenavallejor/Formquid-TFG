package com.formquid.auth.dto;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;

/**
 * Datos básicos del usuario autenticado enviados al frontend.
 *
 * No incluye información sensible como la contraseña cifrada.
 */
public record AuthUserResponseDto(
    Long id,
    String email,
    String name,
    UserRole role
) {

    /**
     * Construye el DTO a partir de la entidad User.
     */
    public static AuthUserResponseDto from(
        User user
    ) {
        return new AuthUserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }
}
