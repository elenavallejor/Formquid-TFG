package com.formquid.survey.dto;

import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveyResponseDto(
    String id,
    UUID ownerId,
    UUID sourceTemplateId,
    String title,
    String description,
    String imageKey,
    SurveyLifecycle lifecycleStatus,
    SurveyAccessMode accessMode,
    SurveyVisibility visibility,
    List<String> tags,
    List<ContentItemResponseDto> contentItems,
    Instant createdAt,
    Instant updatedAt
) {
}
