package com.formquid.auth.dto;

/**
 * Respuesta devuelta tras un inicio de sesión correcto.
 */
public record LoginResponseDto(
    AuthUserResponseDto user
) {
}