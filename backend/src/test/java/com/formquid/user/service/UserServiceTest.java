package com.formquid.user.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.notification.service.NotificationService;
import com.formquid.common.exception.ApiException;
import com.formquid.user.dto.DeleteOwnAccountRequestDto;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCollaboratorRepository collaboratorRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void updateRoleAllowsMoreThanOneSuperadmin() {
        User user = User.builder()
            .id(2L)
            .role(UserRole.ADMIN)
            .email("admin@example.com")
            .name("Admin")
            .institution("FormQuid")
            .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserResponseDto response = userService.updateRole(2L, UserRole.SUPERADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.SUPERADMIN);
        assertThat(response.role()).isEqualTo(UserRole.SUPERADMIN);
    }

    @Test
    void deleteMyAccount_rejectsOwnerWithSurveys() {
        User user = User.builder().id(7L).role(UserRole.EDITOR).passwordHash("hash").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        when(surveyRepository.countByOwnerId(7L)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteMyAccount(
            7L, new DeleteOwnAccountRequestDto("current")
        )).isInstanceOf(ApiException.class).hasMessageContaining("encuestas propias");
        verify(userRepository, never()).delete(user);
    }

    @Test
    void updateRole_keepsAtLeastOneSuperadmin() {
        User user = User.builder().id(2L).role(UserRole.SUPERADMIN).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleAndIdNot(UserRole.SUPERADMIN, 2L)).thenReturn(0L);

        assertThatThrownBy(() -> userService.updateRole(2L, UserRole.ADMIN))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("siempre un superadministrador");
        assertThat(user.getRole()).isEqualTo(UserRole.SUPERADMIN);
    }
}
