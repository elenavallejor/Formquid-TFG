package com.formquid.elementrequest.controller;

import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.service.ElementRequestService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/element-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EDITOR')")
public class ElementRequestController {

    private final ElementRequestService elementRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ElementRequestResponseDto create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateElementRequestDto request
    ) {
        return elementRequestService.create(request, user);
    }
}
