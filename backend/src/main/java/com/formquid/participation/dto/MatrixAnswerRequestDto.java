package com.formquid.participation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para una respuesta de fila de matriz.
 */
public record MatrixAnswerRequestDto(

    @NotBlank(message = "La fila de matriz es obligatoria")
    @Size(max = 120, message = "La clave de la fila no puede superar los 120 caracteres")
    String rowKey,

    @NotBlank(message = "El valor de la fila es obligatorio")
    @Size(max = 255, message = "El valor de la fila no puede superar los 255 caracteres")
    String value,

    Integer scoreValue
) {}
