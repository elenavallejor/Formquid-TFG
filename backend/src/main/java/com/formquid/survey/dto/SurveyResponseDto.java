package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida para una encuesta completa en la zona privada.
 */
public record SurveyResponseDto(
    Long id,
    String publicCode,
    String title,
    String description,
    String imageUrl,
    SurveyStatus status,
    Long ownerUserId,
    String ownerName,
    List<String> tagNames,
    List<SurveyBlockResponseDto> blocks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt
) {}
