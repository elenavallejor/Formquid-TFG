package com.formquid.collaboration.controller;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.service.CollaborationService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collaboration-requests")
@RequiredArgsConstructor
public class CollaborationOverviewController {

    private final CollaborationService collaborationService;

    @GetMapping
    public List<CollaborationRequestResponseDto> findForUser(
        @AuthenticationPrincipal User user
    ) {
        return collaborationService.findRequestsForUser(user.getId());
    }
}
