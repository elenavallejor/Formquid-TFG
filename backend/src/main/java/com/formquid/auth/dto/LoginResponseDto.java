package com.formquid.auth.dto;

/**
 * DTO devuelto tras un inicio de sesión correcto.
 */
public record LoginResponseDto(
    String token,
    String tokenType,
    AuthUserResponseDto user
) {

    /**
     * Construye una respuesta utilizando el esquema Bearer.
     */
    public LoginResponseDto(
        String token,
        AuthUserResponseDto user
    ) {
        this(
            token,
            "Bearer",
            user
        );
    }
}
