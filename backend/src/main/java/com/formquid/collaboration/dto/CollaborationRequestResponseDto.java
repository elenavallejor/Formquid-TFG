package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;
import com.formquid.collaboration.entity.CollaborationPermission;

import java.time.Instant;

public record CollaborationRequestResponseDto(
    Long id,
    Long surveyId,
    String surveyTitle,
    Long userId,
    String userName,
    String userEmail,
    Long ownerId,
    String ownerName,
    String ownerEmail,
    CollaborationRequestType requestType,
    CollaborationPermission permission,
    String message,
    Instant createdAt
) {
}
