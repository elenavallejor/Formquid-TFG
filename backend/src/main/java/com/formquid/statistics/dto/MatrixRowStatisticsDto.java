package com.formquid.statistics.dto;

import java.util.List;

public record MatrixRowStatisticsDto(
    Long rowId,
    String label,
    long answers,
    NumericStatisticsDto numericStatistics,
    List<ValueDistributionDto> valueDistribution,
    Double normalizedScore
) {
}
