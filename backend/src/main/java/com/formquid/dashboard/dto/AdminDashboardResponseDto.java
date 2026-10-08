package com.formquid.dashboard.dto;

/**
 * Contadores necesarios para construir el panel de administración.
 */
public record AdminDashboardResponseDto(
    long pendingElementRequests,
    long surveyCount,
    long pendingRegistrationRequests
) {
}
