package com.formquid.template.dto;

import com.formquid.template.entity.TemplateStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida para una plantilla reutilizable.
 */
public record SurveyTemplateResponseDto(
    Long id,
    String name,
    String description,
    TemplateStatus status,
    Long createdByUserId,
    List<TemplateBlockResponseDto> blocks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
