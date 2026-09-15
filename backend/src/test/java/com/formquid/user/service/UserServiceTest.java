package com.formquid.user.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.notification.service.NotificationService;
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
}
