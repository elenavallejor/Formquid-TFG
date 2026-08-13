package com.formquid.statistics.dto;

import java.math.BigDecimal;

/**
 * Estadística de una opción de respuesta.
 */
public record ChoiceOptionStatisticsDto(
    String storedValue,
    String label,
    long count,
    BigDecimal percentage
) {}