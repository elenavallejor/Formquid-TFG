package com.formquid.auth.dto;

/**
 * Resultado interno del proceso de autenticación.
 */
public record LoginResult(
    String token,
    AuthUserResponseDto user
) {
}