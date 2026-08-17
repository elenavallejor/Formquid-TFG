package com.formquid.survey.dto;

import java.util.List;

/**
 * DTO público resumido para mostrar encuestas disponibles en el inicio.
 */
public record PublicSurveySummaryDto(
    String publicCode,
    String title,
    String description,
    String imageUrl,
    List<String> tagNames
) {}
