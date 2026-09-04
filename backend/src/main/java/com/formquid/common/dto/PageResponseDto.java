package com.formquid.common.dto;

import java.util.List;

/**
 * Representa una página de resultados expuesta por la API.
 */
public record PageResponseDto<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}