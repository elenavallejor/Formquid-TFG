package com.formquid.dashboard.dto;

import com.formquid.survey.dto.AdminSurveySummaryDto;

import java.util.List;

/**
 * Resumen mínimo necesario para construir el panel de administración.
 */
public record AdminDashboardResponseDto(
    long pendingElementRequests,
    long surveyCount,
    long pendingRegistrationRequests,
    List<AdminSurveySummaryDto> recentSurveys
) {
}
