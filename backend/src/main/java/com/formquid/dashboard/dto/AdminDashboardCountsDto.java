package com.formquid.dashboard.dto;

/**
 * Contadores agregados del panel de administración.
 */
public record AdminDashboardCountsDto(
    long pendingElementRequests,
    long surveyCount,
    long pendingRegistrationRequests
) {
}
