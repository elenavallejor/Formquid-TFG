package com.formquid.elementrequest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.formquid.elementrequest.entity.ElementRequestStatus;
import com.formquid.elementrequest.entity.ElementRequestType;

import java.time.LocalDateTime;

/**
 * DTO de salida de una solicitud de elemento.
 */
public record ElementRequestResponseDto(
    Long id,
    ElementRequestType type,
    ElementRequestStatus status,
    String title,
    String description,
    String justification,
    JsonNode requestPayload,
    Long requestedByUserId,
    String requestedByName,
    Long resolvedByUserId,
    String resolvedByName,
    String resolutionMessage,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
