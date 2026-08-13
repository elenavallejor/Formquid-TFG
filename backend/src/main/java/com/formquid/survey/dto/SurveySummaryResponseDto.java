package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO resumido de encuesta.
 *
 * Se usa en listados internos donde no hace falta devolver bloques
 * ni preguntas. Esto hace que las respuestas sean más ligeras.
 */
public record SurveySummaryResponseDto(
    Long id,
    String publicCode,
    String title,
    String description,
    String imageUrl,
    SurveyStatus status,
    Long ownerId,
    String ownerName,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt
) {}