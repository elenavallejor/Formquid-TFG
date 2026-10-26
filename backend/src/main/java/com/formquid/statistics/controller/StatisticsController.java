package com.formquid.statistics.controller;

import com.formquid.statistics.dto.SurveyStatisticsResponseDto;
import com.formquid.statistics.service.StatisticsService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public SurveyStatisticsResponseDto getSurveyStatistics(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user
    ) {
        return statisticsService.getSurveyStatistics(surveyId, user);
    }
}
