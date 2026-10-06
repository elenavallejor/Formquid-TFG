package com.formquid.participation.dto;

public record SelectedOptionResponseDto(
    Long optionId,
    String label,
    String otherText
) {
}
