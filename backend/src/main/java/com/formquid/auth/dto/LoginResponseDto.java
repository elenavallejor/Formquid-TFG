package com.formquid.auth.dto;

/**
 * DTO de salida devuelto tras un inicio de sesión correcto.
 */
public record LoginResponseDto(

    /**
     * Token JWT generado por el backend.
     */
    String token,

    /**
     * Tipo de token utilizado en la cabecera Authorization.
     */
    String tokenType,

    /**
     * Datos básicos del usuario autenticado.
     */
    AuthUserResponseDto user

) {
}