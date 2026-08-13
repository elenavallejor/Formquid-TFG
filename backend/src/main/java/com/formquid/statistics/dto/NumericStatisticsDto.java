package com.formquid.statistics.dto;

import java.math.BigDecimal;

/**
 * Estadísticas agregadas para preguntas numéricas o de escala.
 */
public record NumericStatisticsDto(
    long count,
    BigDecimal average,
    BigDecimal min,
    BigDecimal max
) {}