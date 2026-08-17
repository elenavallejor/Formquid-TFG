package com.formquid.statistics.dto;

import java.math.BigDecimal;

/**
 * Estadísticas de una fila de una pregunta de matriz.
 */
public record MatrixRowStatisticsDto(
    String rowKey,
    String label,
    long count,
    BigDecimal average,
    BigDecimal min,
    BigDecimal max
) {}
