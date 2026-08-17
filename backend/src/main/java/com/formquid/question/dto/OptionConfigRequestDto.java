package com.formquid.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada de una opción de respuesta.
 */
public record OptionConfigRequestDto(

    @NotBlank(message = "El texto de la opción es obligatorio")
    @Size(max = 255, message = "La opción no puede superar los 255 caracteres")
    String label,

    @Size(max = 255, message = "El valor interno no puede superar los 255 caracteres")
    String storedValue,

    Integer scoreValue,

    Boolean nonResponseOption,

    Boolean allowsFreeText

) {
}
