package com.formquid.collaboration.dto;

import jakarta.validation.constraints.Size;

public record CreatePublicationRequestDto(
    @Size(max = 1000) String message
) {
}
