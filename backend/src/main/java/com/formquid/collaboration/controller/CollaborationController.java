package com.formquid.collaboration.controller;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.CreatePublicationRequestDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.service.CollaborationService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/surveys/{surveyId}")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final SurveyService surveyService;

    @GetMapping("/collaborators")
    public List<SurveyCollaboratorResponseDto> findCollaborators(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user
    ) {
        ensureCollaborationAccess(surveyId, user);
        return collaborationService.findCollaborators(surveyId);
    }

    @GetMapping("/collaboration-requests")
    public List<CollaborationRequestResponseDto> findRequests(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user
    ) {
        ensureCollaborationAccess(surveyId, user);
        return collaborationService.findRequests(surveyId);
    }

    @PostMapping("/collaboration-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public CollaborationRequestResponseDto createRequest(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateCollaborationRequestDto request
    ) {
        return collaborationService.request(surveyId, user.getId(), request);
    }

    @PostMapping("/collaboration-requests/{requestId}/accept")
    public SurveyCollaboratorResponseDto accept(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal User user
    ) {
        return collaborationService.accept(requestId, user.getId());
    }

    @DeleteMapping("/collaboration-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal User user
    ) {
        collaborationService.reject(requestId, user.getId());
    }

    @DeleteMapping("/collaborators/{collaboratorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCollaborator(
        @PathVariable String surveyId,
        @PathVariable UUID collaboratorId,
        @AuthenticationPrincipal User user
    ) {
        collaborationService.removeCollaborator(surveyId, collaboratorId, user.getId());
    }

    @PostMapping("/publication-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID requestPublication(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreatePublicationRequestDto request
    ) {
        return collaborationService.requestPublication(surveyId, user.getId(), request.message());
    }

    @PostMapping("/publication-requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptPublication(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal User user
    ) {
        collaborationService.acceptPublication(requestId, user.getId());
    }

    @DeleteMapping("/publication-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPublication(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal User user
    ) {
        collaborationService.rejectPublication(requestId, user.getId());
    }

    private void ensureCollaborationAccess(String surveyId, User user) {
        Survey survey = surveyService.getSurvey(surveyId);
        surveyService.ensureCanViewResults(survey, user);
    }
}
