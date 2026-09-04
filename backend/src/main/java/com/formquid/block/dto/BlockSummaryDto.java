package com.formquid.block.dto;

import java.time.Instant;

/**
 * Contiene los datos necesarios para listar un bloque del catálogo.
 */
public record BlockSummaryDto(
    String id,
    String name,
    String description,
    String creatorName,
    long questionCount,
    Instant updatedAt
) {
}