package com.formquid.registration.dto;

import com.formquid.user.entity.UserGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos enviados por una persona que solicita registrarse como editor.
 *
 * Las validaciones se ejecutan antes de entrar en la capa de servicio.
 */
public record CreateRegistrationRequestDto(

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(
        max = 150,
        message = "El nombre completo no puede superar los 150 caracteres"
    )
    String name,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(
        max = 255,
        message = "El correo electrónico no puede superar los 255 caracteres"
    )
    String email,

    @NotNull(message = "El género es obligatorio")
    UserGender gender,

    @NotBlank(message = "La institución es obligatoria")
    @Size(
        max = 150,
        message = "La institución no puede superar los 150 caracteres"
    )
    String institution,

    @NotBlank(message = "La motivación de la solicitud es obligatoria")
    @Size(
        max = 1000,
        message = "La motivación no puede superar los 1000 caracteres"
    )
    String motivation

) {}
