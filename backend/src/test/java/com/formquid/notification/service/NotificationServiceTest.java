package com.formquid.notification.service;

import com.formquid.common.exception.ApiException;
import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.repository.NotificationRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock UserRepository users;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks NotificationService service;

    @Test
    void paginatesAndFiltersNotificationsInTheRepository() {
        var pageable = PageRequest.of(0, 4);
        var notification = new NotificationResponseDto(
            1L, NotificationType.GENERAL, NotificationChannel.INTERNAL, "Aviso", "Descripción",
            NotificationTargetType.NONE, null, Instant.now(), null
        );
        when(repository.findResponses(1L, true, NotificationType.GENERAL, pageable))
            .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));

        var result = service.findForUser(1L, 0, 4, true, NotificationType.GENERAL);

        assertThat(result.content()).containsExactly(notification);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(repository).findResponses(1L, true, NotificationType.GENERAL, pageable);
    }

    @Test
    void rejectsAnInvalidNotificationPageSize() {
        assertThatThrownBy(() -> service.findForUser(1L, 0, 21, false, null))
            .isInstanceOf(ApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void priorityEventHasOneRecordForEmailAndInbox() {
        User user = User.builder().id(1L).email("editor@example.com").build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.createInternal(user, NotificationType.COLLABORATION_REVOKED, "Revocada", "Encuesta A");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getRecipientUser()).isSameAs(user);
        verify(repository, times(1)).save(any());
        var email = ArgumentCaptor.forClass(EmailRequested.class);
        verify(eventPublisher).publishEvent(email.capture());
        assertThat(email.getValue()).isEqualTo(new EmailRequested("editor@example.com", "Revocada", "Encuesta A"));
    }

    @Test
    void ordinaryInvitationDoesNotQueueEmail() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.createInternal(User.builder().id(1L).build(),
            NotificationType.COLLABORATION_INVITATION, "Invitación", "Encuesta A");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.INTERNAL);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void registrationEmailDoesNotRequireAnAccount() {
        service.sendPriorityEmail("PERSON@example.com", NotificationType.REGISTRATION_REJECTED, "Alta", "Rechazada");

        var email = ArgumentCaptor.forClass(EmailRequested.class);
        verify(eventPublisher).publishEvent(email.capture());
        assertThat(email.getValue()).isEqualTo(new EmailRequested("person@example.com", "Alta", "Rechazada"));
        verifyNoInteractions(repository);
        assertThatThrownBy(() -> service.sendPriorityEmail("person@example.com", NotificationType.GENERAL, "Aviso", "Mensaje"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotDeleteUnreadNotification() {
        when(repository.findByIdAndRecipientUserId(3L, 1L)).thenReturn(Optional.of(new Notification()));
        assertThatThrownBy(() -> service.delete(3L, 1L)).isInstanceOf(ApiException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void readingEmailNoticeIsIdempotentAndAllowsPermanentDeletion() {
        Notification notice = Notification.builder().id(3L).channel(NotificationChannel.INTERNAL).build();
        when(repository.findByIdAndRecipientUserId(3L, 1L)).thenReturn(Optional.of(notice));
        service.markAsRead(3L, 1L);
        Instant firstRead = notice.getReadAt();
        service.markAsRead(3L, 1L);
        assertThat(firstRead).isNotNull().isEqualTo(notice.getReadAt());
        service.delete(3L, 1L);
        verify(repository).delete(notice);
    }

    @Test
    void cannotReadOrDeleteAnotherUsersNotification() {
        when(repository.findByIdAndRecipientUserId(3L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead(3L, 2L)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.delete(3L, 2L)).isInstanceOf(ApiException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void markAllIsScopedToCurrentUser() {
        service.markAllAsRead(9L);
        verify(repository).markAllAsRead(eq(9L), any(Instant.class));
    }
}
