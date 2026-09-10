package com.formquid.notification.controller;

import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.dto.UnreadNotificationCountResponseDto;
import com.formquid.notification.service.NotificationService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestiona las operaciones relacionadas con las notificaciones del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponseDto> findMine(@AuthenticationPrincipal User user) {
        return notificationService.findForUser(user.getId());
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponseDto countUnread(@AuthenticationPrincipal User user) {
        return new UnreadNotificationCountResponseDto(notificationService.countUnread(user.getId()));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponseDto markAsRead(@PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        return notificationService.markAsRead(notificationId, user.getId());
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        notificationService.delete(notificationId, user.getId());
    }
}