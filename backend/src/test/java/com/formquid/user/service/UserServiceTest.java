package com.formquid.user.service;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.notification.service.NotificationService;
import com.formquid.common.exception.ApiException;
import com.formquid.user.dto.DeleteOwnAccountRequestDto;
import com.formquid.user.dto.UpdateAccountRequestDto;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private ProfilePhotoStorage profilePhotoStorage;

    @InjectMocks
    private UserService userService;

    @Test
    void uploadProfilePhotoReplacesPreviousPhoto() {
        User user = User.builder().id(7L).profileImageKey("/api/v1/users/7/photo/old.png").build();
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(profilePhotoStorage.save(7L, file)).thenReturn("/api/v1/users/7/photo/new.png");

        UserResponseDto response = userService.uploadProfilePhoto(7L, file);

        assertThat(response.profileImageKey()).isEqualTo("/api/v1/users/7/photo/new.png");
        verify(profilePhotoStorage).deleteAfterCommit(7L, "/api/v1/users/7/photo/old.png");
    }

    @Test
    void removeProfilePhotoClearsReferenceAndDeletesFileAfterCommit() {
        User user = User.builder().id(7L).profileImageKey("/api/v1/users/7/photo/old.png").build();
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        UserResponseDto response = userService.removeProfilePhoto(7L);

        assertThat(response.profileImageKey()).isNull();
        verify(profilePhotoStorage).deleteAfterCommit(7L, "/api/v1/users/7/photo/old.png");
    }

    @Test
    void getProfilePhotoRejectsOutdatedFilename() {
        User user = User.builder().id(7L).profileImageKey("/api/v1/users/7/photo/current.png").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getProfilePhoto(7L, "previous.png"))
            .isInstanceOf(ApiException.class);
        verify(profilePhotoStorage, never()).read(any());
    }

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
    void updateAccountStoresCountryForExistingUser() {
        User user = User.builder().id(7L).name("Ana").institution("Asociación").passwordHash("hash").build();
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);

        var result = userService.updateMyAccount(7L, new UpdateAccountRequestDto(
            new UpdateAccountRequestDto.ProfileData("Ana", "Asociación", "ES"), null, null, "current"
        ));

        assertThat(user.getCountry()).isEqualTo("ES");
        assertThat(result.tokenRenewalRequired()).isFalse();
    }

    @Test
    void updateAccountRenewsTokenAfterPasswordChange() {
        User user = User.builder().id(7L).email("ana@example.com")
            .passwordHash("old-hash").build();
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-secret", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

        var result = userService.updateMyAccount(7L, new UpdateAccountRequestDto(
            null, null, new UpdateAccountRequestDto.PasswordData("new-secret"), "current"
        ));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(result.tokenRenewalRequired()).isTrue();
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

    @Test
    void transferSurveyOwnership_withValidPassword_makesPreviousOwnerEditorCollaborator() {
        User previousOwner = User.builder().id(7L).passwordHash("hash").build();
        User newOwner = User.builder().id(9L).build();
        Survey survey = Survey.builder().id(12L).owner(previousOwner).title("Hábitos digitales").build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        when(collaboratorRepository.existsByIdSurveyIdAndIdUserId(12L, 9L)).thenReturn(true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(newOwner));

        userService.transferSurveyOwnership(12L, 7L, 9L, "current");

        assertThat(survey.getOwner()).isSameAs(newOwner);
        verify(collaboratorRepository).deleteByIdSurveyIdAndIdUserId(12L, 9L);
        ArgumentCaptor<SurveyCollaborator> collaborator = ArgumentCaptor.forClass(SurveyCollaborator.class);
        verify(collaboratorRepository).save(collaborator.capture());
        assertThat(collaborator.getValue().getId().getSurveyId()).isEqualTo(12L);
        assertThat(collaborator.getValue().getId().getUserId()).isEqualTo(7L);
        assertThat(collaborator.getValue().getUser()).isSameAs(previousOwner);
        assertThat(collaborator.getValue().getPermission()).isEqualTo(CollaborationPermission.EDIT);
    }

    @Test
    void transferSurveyOwnership_withWrongPassword_keepsOwnerAndCollaborators() {
        User previousOwner = User.builder().id(7L).passwordHash("hash").build();
        Survey survey = Survey.builder().id(12L).owner(previousOwner).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> userService.transferSurveyOwnership(12L, 7L, 9L, "wrong"))
            .isInstanceOf(ApiException.class)
            .hasMessage("La contraseña actual no es correcta");

        assertThat(survey.getOwner()).isSameAs(previousOwner);
        verify(collaboratorRepository, never()).save(any());
        verify(collaboratorRepository, never()).deleteByIdSurveyIdAndIdUserId(12L, 9L);
    }
}
