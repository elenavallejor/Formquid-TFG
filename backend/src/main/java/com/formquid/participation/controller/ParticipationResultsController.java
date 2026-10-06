package com.formquid.participation.controller;

import com.formquid.common.dto.PageResponseDto;
import com.formquid.participation.dto.ParticipationDetailResponseDto;
import com.formquid.participation.dto.ParticipationSummaryResponseDto;
import com.formquid.participation.service.ParticipationService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la consulta autenticada y de solo lectura de participaciones individuales.
 */
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/participations")
@RequiredArgsConstructor
public class ParticipationResultsController {

    private final ParticipationService participationService;

    @GetMapping
    public PageResponseDto<ParticipationSummaryResponseDto> findPage(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return participationService.findParticipationPage(surveyId, user, page, size);
    }

    @GetMapping("/{participationId}")
    public ParticipationDetailResponseDto getDetail(
        @PathVariable Long surveyId,
        @PathVariable Long participationId,
        @AuthenticationPrincipal User user
    ) {
        return participationService.getParticipationDetail(surveyId, participationId, user);
    }
}
