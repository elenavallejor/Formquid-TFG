package com.formquid.dashboard.service;

import com.formquid.dashboard.dto.AdminDashboardCountsDto;
import com.formquid.dashboard.dto.AdminDashboardResponseDto;
import com.formquid.dashboard.repository.AdminDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardRepository dashboardRepository;

    /** Construye los contadores agregados del panel administrativo. */
    @Transactional(readOnly = true)
    public AdminDashboardResponseDto getDashboard() {
        AdminDashboardCountsDto counts = dashboardRepository.getCounts();

        return new AdminDashboardResponseDto(
            counts.pendingElementRequests(),
            counts.surveyCount(),
            counts.pendingRegistrationRequests()
        );
    }
}
