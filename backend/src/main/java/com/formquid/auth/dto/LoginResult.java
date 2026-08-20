package com.formquid.auth.dto;

/**
 * Resultado interno del proceso de autenticación.
 *
 * Contiene el token únicamente para que el backend pueda
 * almacenarlo en una cookie HttpOnly.
 */
public record LoginResult(
    String token,
    AuthUserResponseDto user
) {
}