package com.formquid.template.dto;

import com.formquid.content.dto.ContentItemResponseDto;

import java.time.Instant;
import java.util.List;

/**
 * Representa el detalle completo de una plantilla y su contenido ordenado.
 */
public record TemplateResponseDto(
    Long id,
    String title,
    String description,
    String imageKey,
    List<ContentItemResponseDto> contentItems,
    Long creatorId,
    String creatorName,
    Instant createdAt,
    Instant updatedAt
) {
}
