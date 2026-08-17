package com.formquid.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para configurar la contraseña inicial de una cuenta aprobada.
 */
public record CompletePasswordSetupRequestDto(

    @NotBlank(message = "El token de configuración es obligatorio")
    @Size(max = 100, message = "El token de configuración no puede superar los 100 caracteres")
    String token,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        min = 8,
        max = 100,
        message = "La contraseña debe tener entre 8 y 100 caracteres"
    )
    String password
) {}
