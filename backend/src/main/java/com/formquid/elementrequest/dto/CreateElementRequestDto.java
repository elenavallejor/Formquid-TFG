package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.ElementRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos necesarios para crear una solicitud de elemento reutilizable.
 *
 * Las validaciones se ejecutan antes de entrar en la capa de servicio.
 */
public record CreateElementRequestDto(

    @NotNull(message = "El tipo de elemento solicitado es obligatorio")
    ElementRequestType type,

    @NotBlank(message = "El título de la solicitud es obligatorio")
    @Size(
        max = 180,
        message = "El título no puede superar los 180 caracteres"
    )
    String title,

    @NotBlank(message = "La descripción de la solicitud es obligatoria")
    @Size(
        max = 2000,
        message = "La descripción no puede superar los 2000 caracteres"
    )
    String description,

    /**
     * Campo opcional para explicar por qué el elemento propuesto
     * resultaría útil en el catálogo.
     */
    @Size(
        max = 1500,
        message = "La justificación no puede superar los 1500 caracteres"
    )
    String justification
) {}
