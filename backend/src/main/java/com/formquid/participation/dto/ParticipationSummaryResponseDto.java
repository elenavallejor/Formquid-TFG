package com.formquid.participation.dto;

import java.time.Instant;

/**
 * Identifica una participación sin exponer la identidad ni el token del participante.
 */
public record ParticipationSummaryResponseDto(
    Long id,
    Instant submittedAt
) {
}
