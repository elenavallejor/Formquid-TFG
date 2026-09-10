package com.formquid.statistics.dto;

import java.math.BigDecimal;

public record NumericStatisticsDto(
    Double average,
    BigDecimal min,
    BigDecimal max,
    Double median,
    Double standardDeviation
) {
}