package com.formquid.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos necesarios para iniciar sesión.
 *
 * Recibe el correo y la contraseña enviados por el usuario
 * desde el formulario de autenticación.
 */
public record LoginRequestDto(
    @Email @NotBlank String email,
    @NotBlank String password
) {
}
