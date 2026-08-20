package com.formquid.auth.dto;

/**
 * Respuesta devuelta tras un inicio de sesión correcto.
 *
 * El JWT no se expone al frontend, ya que se almacena
 * en una cookie HttpOnly.
 */
public record LoginResponseDto(
    AuthUserResponseDto user
) {
}