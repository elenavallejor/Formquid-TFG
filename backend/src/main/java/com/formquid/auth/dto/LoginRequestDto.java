package com.formquid.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada utilizado para iniciar sesión en FormQuid.
 */
public record LoginRequestDto(

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    @Size(
        max = 255,
        message = "El email no puede superar los 255 caracteres"
    )
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        max = 100,
        message = "La contraseña no puede superar los 100 caracteres"
    )
    String password

) {}
