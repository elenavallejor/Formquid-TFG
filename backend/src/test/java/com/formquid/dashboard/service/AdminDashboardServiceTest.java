package com.formquid.dashboard.service;

import com.formquid.dashboard.dto.AdminDashboardCountsDto;
import com.formquid.dashboard.repository.AdminDashboardRepository;
import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.repository.SurveyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminDashboardRepository dashboardRepository;
    @Mock
    private SurveyRepository surveyRepository;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void getDashboard_returnsCountsAndFiveMostRecentlyUpdatedSurveys() {
        var counts = new AdminDashboardCountsDto(3, 12, 4);
        var recent = List.of(mock(AdminSurveySummaryDto.class));
        when(dashboardRepository.getCounts()).thenReturn(counts);
        when(surveyRepository.findRecentAdminSummaries(org.mockito.ArgumentMatchers.any())).thenReturn(recent);

        var response = dashboardService.getDashboard();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(surveyRepository).findRecentAdminSummaries(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("updatedAt").isDescending()).isTrue();
        assertThat(response.pendingElementRequests()).isEqualTo(3);
        assertThat(response.surveyCount()).isEqualTo(12);
        assertThat(response.pendingRegistrationRequests()).isEqualTo(4);
        assertThat(response.recentSurveys()).isSameAs(recent);
    }
}
