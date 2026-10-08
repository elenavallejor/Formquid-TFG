package com.formquid.dashboard.service;

import com.formquid.dashboard.dto.AdminDashboardCountsDto;
import com.formquid.dashboard.repository.AdminDashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminDashboardRepository dashboardRepository;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void getDashboard_returnsAggregatedCounts() {
        var counts = new AdminDashboardCountsDto(3, 12, 4);
        when(dashboardRepository.getCounts()).thenReturn(counts);

        var response = dashboardService.getDashboard();

        assertThat(response.pendingElementRequests()).isEqualTo(3);
        assertThat(response.surveyCount()).isEqualTo(12);
        assertThat(response.pendingRegistrationRequests()).isEqualTo(4);
    }
}
