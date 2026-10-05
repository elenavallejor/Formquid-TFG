package com.formquid.statistics.dto;

import java.math.BigDecimal;

/**
 * Representa la frecuencia de un valor dentro de una pregunta de escala.
 */
public record ValueDistributionDto(
    BigDecimal value,
    long count,
    double percentage
) {
}
