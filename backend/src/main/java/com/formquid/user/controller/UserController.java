package com.formquid.user.controller;

import com.formquid.user.dto.TransferOwnershipRequestDto;
import com.formquid.user.dto.UpdateUserRoleRequestDto;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDto me(@AuthenticationPrincipal User user) {
        return userService.getProfile(user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public List<UserResponseDto> findAll() {
        return userService.findAll();
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public UserResponseDto updateRole(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRoleRequestDto request
    ) {
        return userService.updateRole(userId, request.role());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPERADMIN')")
    public void delete(@PathVariable UUID userId) {
        userService.delete(userId);
    }

    @PostMapping("/surveys/{surveyId}/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody TransferOwnershipRequestDto request
    ) {
        userService.transferSurveyOwnership(surveyId, user.getId(), request.newOwnerId());
    }
}
