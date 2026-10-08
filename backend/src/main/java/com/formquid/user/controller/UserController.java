package com.formquid.user.controller;

import com.formquid.auth.security.JwtCookieService;
import com.formquid.auth.security.JwtService;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.user.dto.DeleteOwnAccountRequestDto;
import com.formquid.user.dto.TransferOwnershipRequestDto;
import com.formquid.user.dto.UpdateAccountRequestDto;
import com.formquid.user.dto.UpdateAccountResult;
import com.formquid.user.dto.UpdateUserRoleRequestDto;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.dto.UserSearchField;
import com.formquid.user.dto.UserSortOrder;
import com.formquid.user.entity.User;
import com.formquid.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Expone las operaciones de usuario, cuenta propia, roles y transferencia de propiedad.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;

    @GetMapping("/me")
    public UserResponseDto me(@AuthenticationPrincipal User user) {
        return userService.getProfile(user);
    }

    @PatchMapping("/me/account")
    public ResponseEntity<UserResponseDto> updateMyAccount(@AuthenticationPrincipal User user, @Valid @RequestBody UpdateAccountRequestDto request) {
        UpdateAccountResult result = userService.updateMyAccount(user.getId(), request);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();

        if (result.emailChanged()) {
            String token = jwtService.generateToken(result.user());
            response.header(HttpHeaders.SET_COOKIE, jwtCookieService.createCookie(token).toString());
        }

        return response.body(userService.toResponse(result.user()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal User user, @Valid @RequestBody DeleteOwnAccountRequestDto request) {
        userService.deleteMyAccount(user.getId(), request);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, jwtCookieService.deleteCookie().toString())
            .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public PageResponseDto<UserResponseDto> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "NAME,EMAIL,INSTITUTION,GENDER,ROLE") Set<UserSearchField> searchFields,
        @RequestParam(defaultValue = "NAME_ASC") UserSortOrder sortOrder
    ) {
        return userService.findAll(page, size, search, searchFields, sortOrder);
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public UserResponseDto updateRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequestDto request) {
        return userService.updateRole(userId, request.role());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPERADMIN')")
    public void delete(@PathVariable Long userId) {
        userService.delete(userId);
    }

    @PostMapping("/surveys/{surveyId}/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(@PathVariable Long surveyId, @AuthenticationPrincipal User user, @Valid @RequestBody TransferOwnershipRequestDto request) {
        userService.transferSurveyOwnership(surveyId, user.getId(), request.newOwnerId());
    }
}
