package com.formquid.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada utilizado para iniciar sesión en FormQuid.
 */
public record LoginRequestDto(

    /**
     * Correo electrónico del usuario registrado.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    String email,

    /**
     * Contraseña introducida por el usuario.
     *
     * Se recibe en texto durante el login y se compara internamente con la contraseña cifrada almacenada.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    String password

) {
}