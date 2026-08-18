package com.formquid.template.dto;

import com.formquid.content.dto.ContentItemResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponseDto(
    UUID id,
    String title,
    String description,
    String imageKey,
    List<ContentItemResponseDto> contentItems,
    Instant createdAt,
    Instant updatedAt
) {
}
