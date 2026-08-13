package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.ElementRequestStatus;
import com.formquid.elementrequest.entity.ElementRequestType;

import java.time.LocalDateTime;

/**
 * DTO de salida de una solicitud de elemento.
 *
 * Incluye los datos de trazabilidad necesarios para mostrar
 * quién creó la solicitud y, cuando corresponda, quién la resolvió.
 */
public record ElementRequestResponseDto(
    Long id,
    ElementRequestType type,
    String title,
    String description,
    String justification,
    Long requestedByUserId,
    String requestedByName,
    ElementRequestStatus status,
    Long resolvedByUserId,
    String resolvedByName,
    String resolutionMessage,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
