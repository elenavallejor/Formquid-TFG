package com.formquid.participation.dto;

/**
 * DTO de salida para una respuesta de fila de matriz.
 */
public record MatrixAnswerResponseDto(
    String rowKey,
    String value,
    Integer scoreValue
) {}