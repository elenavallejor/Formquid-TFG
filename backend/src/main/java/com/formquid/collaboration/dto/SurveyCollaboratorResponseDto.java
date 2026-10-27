package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationPermission;

import java.time.Instant;

public record SurveyCollaboratorResponseDto(
    Long surveyId,
    Long userId,
    String email,
    String name,
    String profileImageKey,
    CollaborationPermission permission,
    Instant joinedAt
) {
}
