package com.formquid.tag.dto;

/**
 * DTO de salida para una etiqueta.
 */
public record TagResponseDto(
    Long id,
    String name
) {}