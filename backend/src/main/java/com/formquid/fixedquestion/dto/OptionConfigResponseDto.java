package com.formquid.fixedquestion.dto;

/**
 * Datos de una opción de respuesta devueltos por la API.
 */
public record OptionConfigResponseDto(
    String label,
    String storedValue,
    Integer scoreValue,
    boolean nonResponseOption,
    boolean allowsFreeText
) {
}