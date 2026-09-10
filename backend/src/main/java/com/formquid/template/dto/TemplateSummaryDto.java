package com.formquid.template.dto;

import java.time.Instant;

/**
 * Contiene los datos necesarios para listar una plantilla del catálogo.
 */
public record TemplateSummaryDto(
    Long id,
    String title,
    String description,
    String imageKey,
    String creatorName,
    long questionCount,
    Instant updatedAt
) {
}