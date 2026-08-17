package com.formquid.block.dto;
import com.formquid.block.entity.BlockStatus;
import java.time.LocalDateTime;

/**
 * Datos de un bloque devueltos por la API.
 */
public record BlockResponseDto(
    Long id,
    String name,
    String description,
    BlockStatus status,
    Long createdByUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
