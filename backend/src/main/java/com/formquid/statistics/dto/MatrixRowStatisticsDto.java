package com.formquid.statistics.dto;

import java.util.UUID;

public record MatrixRowStatisticsDto(
    UUID rowId,
    String label,
    long answers,
    NumericStatisticsDto numericStatistics,
    Double normalizedScore
) {
}
