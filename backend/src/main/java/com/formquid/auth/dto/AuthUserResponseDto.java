package com.formquid.auth.dto;

import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;

/**
 * Datos básicos del usuario autenticado enviados al frontend.
 *
 * No incluye información sensible como la contraseña cifrada.
 */
public record AuthUserResponseDto(

    /**
     * Identificador numérico del usuario.
     */
    Long id,

    /**
     * Nombre completo del usuario.
     */
    String fullName,

    /**
     * Correo electrónico del usuario.
     */
    String email,

    /**
     * Rol asignado en el sistema.
     */
    UserRole role,

    /**
     * Género indicado por el usuario.
     */
    UserGender gender

) {
}