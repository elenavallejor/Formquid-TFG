package com.formquid.registration.service;

import com.formquid.common.exception.ApiException;
import com.formquid.notification.service.NotificationService;
import com.formquid.registration.dto.CreateRegistrationRequestDto;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestServiceTest {

    @Mock
    private RegistrationRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RegistrationRequestService registrationService;

    @Test
    void create_normalizesEmailAndNeverStoresPlainPassword() {
        var request = new CreateRegistrationRequestDto(
            "  CHILD@EXAMPLE.COM ", "  Ana  ", UserGender.FEMALE,
            "  Colegio  ", "  Quiero crear encuestas  ", "plain-secret"
        );
        when(passwordEncoder.encode("plain-secret")).thenReturn("password-hash");
        when(requestRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            RegistrationRequest saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        var response = registrationService.create(request);

        ArgumentCaptor<RegistrationRequest> entity = ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(requestRepository).save(entity.capture());
        assertThat(entity.getValue().getEmail()).isEqualTo("child@example.com");
        assertThat(entity.getValue().getPasswordHash()).isEqualTo("password-hash");
        assertThat(entity.getValue().getName()).isEqualTo("Ana");
        assertThat(response.id()).isEqualTo(5L);
        verify(notificationService).notifyAdminsAboutRegistrationRequest(entity.getValue());
    }

    @Test
    void create_rejectsEmailAlreadyPending() {
        when(requestRepository.existsByEmail("person@example.com")).thenReturn(true);
        var request = new CreateRegistrationRequestDto(
            "PERSON@example.com", "Ana", UserGender.OTHER, "Colegio", "Motivación", "secret123"
        );

        assertThatThrownBy(() -> registrationService.create(request))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("pendiente");
    }

    @Test
    void approve_createsEditorWithExistingPasswordHash() {
        var pending = RegistrationRequest.builder()
            .id(6L)
            .email("editor@example.com")
            .name("Editora")
            .gender(UserGender.FEMALE)
            .institution("Centro")
            .passwordHash("password-hash")
            .build();
        when(requestRepository.findById(6L)).thenReturn(Optional.of(pending));
        when(userRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(21L);
            return user;
        });

        Long userId = registrationService.approve(6L);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo(UserRole.EDITOR);
        assertThat(user.getValue().getPassword()).isEqualTo("password-hash");
        assertThat(userId).isEqualTo(21L);
        verify(requestRepository).delete(pending);
    }
}
