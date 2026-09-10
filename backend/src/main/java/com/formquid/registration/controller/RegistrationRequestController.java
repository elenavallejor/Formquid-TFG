package com.formquid.registration.controller;

import com.formquid.registration.dto.CreateRegistrationRequestDto;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.service.RegistrationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registration-requests")
@RequiredArgsConstructor
public class RegistrationRequestController {

    private final RegistrationRequestService registrationRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationRequestResponseDto create(@Valid @RequestBody CreateRegistrationRequestDto request) {
        return registrationRequestService.create(request);
    }
}