package com.formquid.block.dto;

import com.formquid.common.Category;
import com.formquid.block.entity.BlockScope;
import com.formquid.content.dto.ContentItemResponseDto;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta con los datos de un bloque.
 */
public record BlockResponseDto(
    Long id,
    BlockScope scope,
    String name,
    String description,
    List<ContentItemResponseDto> contentItems,
    Long creatorId,
    String creatorName,
    Instant createdAt,
    Instant updatedAt,
    Category category
) {
}
