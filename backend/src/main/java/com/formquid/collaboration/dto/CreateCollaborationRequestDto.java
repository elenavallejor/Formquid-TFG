package com.formquid.collaboration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para invitar a un editor a colaborar en una encuesta.
 *
 * El editor puede identificarse por id o por correo electrónico.
 * La validación de que solo venga uno de los dos campos se realiza en servicio
 * para poder devolver un mensaje de error claro.
 */
public record CreateCollaborationRequestDto(

    @Positive(message = "El identificador del editor debe ser positivo")
    Long invitedEditorId,

    @Email(message = "El correo electrónico del editor no tiene un formato válido")
    @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
    String invitedEditorEmail,

    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
    String message
) {}