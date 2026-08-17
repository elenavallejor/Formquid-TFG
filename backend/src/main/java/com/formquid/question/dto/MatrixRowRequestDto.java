package com.formquid.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada de una fila de matriz.
 */
public record MatrixRowRequestDto(

    @Size(max = 100, message = "La clave de la fila no puede superar los 100 caracteres")
    String key,

    @NotBlank(message = "El texto de la fila es obligatorio")
    @Size(max = 255, message = "El texto de la fila no puede superar los 255 caracteres")
    String label

) {
}
