package com.formquid.common.dto;

/**
 * DTO genérico para devolver contadores simples.
 *
 * Se reutiliza en endpoints que solo necesitan devolver un número,
 * evitando crear un DTO distinto para cada contador.
 */
public record CountResponseDto(
    long count
) {}