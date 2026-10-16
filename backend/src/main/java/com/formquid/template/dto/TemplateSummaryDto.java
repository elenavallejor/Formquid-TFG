package com.formquid.template.dto;

import com.formquid.common.Category;
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
    Instant createdAt,
    Instant updatedAt,
    Category category
) {
}
