package com.formquid.survey.dto;

import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyFontFamily;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;

import java.time.Instant;
import java.util.List;

/**
 * Representa el detalle completo de una encuesta, incluido su contenido.
 */
public record SurveyResponseDto(
    Long id,
    String publicCode,
    Long ownerId,
    String title,
    String description,
    String imageKey,
    SurveyFontFamily fontFamily,
    SurveyLifecycle lifecycleStatus,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    List<String> tags,
    List<ContentItemResponseDto> contentItems,
    Instant createdAt,
    Instant updatedAt
) {
}
