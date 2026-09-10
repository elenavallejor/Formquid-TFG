package com.formquid.statistics.dto;

public record MatrixRowStatisticsDto(
    Long rowId,
    String label,
    long answers,
    NumericStatisticsDto numericStatistics,
    Double normalizedScore
) {
}