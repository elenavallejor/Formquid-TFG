package com.formquid.dashboard.service;

import com.formquid.dashboard.dto.AdminDashboardCountsDto;
import com.formquid.dashboard.dto.AdminDashboardResponseDto;
import com.formquid.dashboard.repository.AdminDashboardRepository;
import com.formquid.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final int RECENT_SURVEYS_LIMIT = 5;

    private final AdminDashboardRepository dashboardRepository;
    private final SurveyRepository surveyRepository;

    /**
     * Construye el panel con dos consultas: una para todos los contadores y
     * otra para las encuestas recientes.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponseDto getDashboard() {
        AdminDashboardCountsDto counts = dashboardRepository.getCounts();

        return new AdminDashboardResponseDto(
            counts.pendingElementRequests(),
            counts.surveyCount(),
            counts.pendingRegistrationRequests(),
            surveyRepository.findRecentAdminSummaries(
                PageRequest.of(
                    0,
                    RECENT_SURVEYS_LIMIT,
                    Sort.by(Sort.Direction.DESC, "updatedAt")
                )
            )
        );
    }
}
