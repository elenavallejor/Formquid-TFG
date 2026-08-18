package com.formquid.statistics.dto;

import java.util.UUID;

public record ChoiceOptionStatisticsDto(
    UUID optionId,
    String label,
    long count,
    double percentage
) {
}
