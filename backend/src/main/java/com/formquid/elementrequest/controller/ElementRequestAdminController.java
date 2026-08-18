package com.formquid.elementrequest.controller;

import com.formquid.elementrequest.dto.ApproveElementRequestResponseDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.service.ElementRequestService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/element-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class ElementRequestAdminController {

    private final ElementRequestService elementRequestService;

    @GetMapping
    public List<ElementRequestResponseDto> findPending() {
        return elementRequestService.findPending();
    }

    @PostMapping("/{requestId}/approve")
    public ApproveElementRequestResponseDto approve(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal User user
    ) {
        return elementRequestService.approve(requestId, user);
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID requestId) {
        elementRequestService.reject(requestId);
    }
}
