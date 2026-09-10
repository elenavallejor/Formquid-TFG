package com.formquid.registration.controller;

import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.service.RegistrationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/registration-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class RegistrationRequestAdminController {

    private final RegistrationRequestService registrationRequestService;

    @GetMapping
    public List<RegistrationRequestResponseDto> findPending() {
        return registrationRequestService.findPending();
    }

    @PostMapping("/{requestId}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public Long approve(@PathVariable Long requestId) {
        return registrationRequestService.approve(requestId);
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long requestId) {
        registrationRequestService.reject(requestId);
    }
}