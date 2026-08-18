package com.formquid.statistics.dto;

public record BooleanStatisticsDto(
    long trueCount,
    long falseCount,
    double truePercentage,
    double falsePercentage
) {
}
