package com.formquid.fixedquestion.dto;

/**
 * Datos de una fila de matriz devueltos por la API.
 */
public record MatrixRowResponseDto(
    String key,
    String label
) {
}