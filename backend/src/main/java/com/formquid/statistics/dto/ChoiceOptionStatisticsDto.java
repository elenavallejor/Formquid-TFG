package com.formquid.statistics.dto;

public record ChoiceOptionStatisticsDto(
    Long optionId,
    String label,
    long count,
    double percentage
) {
}