package com.formquid.user.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.dto.DeleteOwnAccountRequestDto;
import com.formquid.user.dto.UpdateAccountRequestDto;
import com.formquid.user.dto.UpdateAccountResult;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona usuarios, cuenta propia, roles y transferencia de propiedad.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyCollaboratorRepository collaboratorRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> findAll(int page, int size) {
        Page<UserResponseDto> users = userRepository.findResponses(createPageRequest(page, size));
        return new PageResponseDto<>(users.getContent(), users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(User user) {
        return userRepository.findResponseById(user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public UpdateAccountResult updateMyAccount(Long id, UpdateAccountRequestDto request) {
        User user = getById(id);
        ensureCurrentPassword(user, request.currentPassword());

        boolean changed = false;
        boolean emailChanged = false;

        if (request.profile() != null) {
            changed = updateProfile(user, request.profile()) || changed;
        }
        if (request.password() != null) {
            changed = updatePassword(user, request.password()) || changed;
        }
        if (request.email() != null) {
            emailChanged = updateEmail(user, request.email());
            changed = emailChanged || changed;
        }
        if (!changed) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se ha detectado ningún cambio en la cuenta");
        }
        return new UpdateAccountResult(user, emailChanged);
    }

    @Transactional
    public void deleteMyAccount(Long id, DeleteOwnAccountRequestDto request) {
        User user = getById(id);
        ensureCurrentPassword(user, request.currentPassword());

        if (surveyRepository.countByOwnerId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No puedes eliminar tu cuenta porque tienes encuestas propias");
        }
        if (user.getRole() == UserRole.SUPERADMIN) {
            ensureAnotherSuperadmin(id);
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserResponseDto updateRole(Long id, UserRole newRole) {
        User user = getById(id);

        if (user.getRole() == UserRole.SUPERADMIN && newRole != UserRole.SUPERADMIN) {
            ensureAnotherSuperadmin(user.getId());
        }
        user.setRole(newRole);
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getById(id);

        if (surveyRepository.countByOwnerId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No puede eliminarse un propietario de encuestas");
        }
        if (user.getRole() == UserRole.SUPERADMIN) {
            ensureAnotherSuperadmin(id);
        }
        userRepository.delete(user);
    }

    @Transactional
    public void transferSurveyOwnership(Long surveyId, Long currentOwnerId, Long newOwnerId) {
        var survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));

        if (!survey.getOwner().getId().equals(currentOwnerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario actual puede transferir la encuesta");
        }

        User newOwner = getById(newOwnerId);
        User previousOwner = survey.getOwner();

        if (!collaboratorRepository.existsByIdSurveyIdAndIdUserId(surveyId, newOwnerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El nuevo propietario debe ser colaborador de la encuesta");
        }

        survey.setOwner(newOwner);
        collaboratorRepository.deleteByIdSurveyIdAndIdUserId(surveyId, newOwnerId);

        notificationService.createSurveyNotification(newOwner, NotificationType.SURVEY_OWNERSHIP_TRANSFERRED, "Propiedad de encuesta transferida", "Ahora eres propietario de la encuesta " + survey.getTitle(), survey);
        notificationService.createSurveyNotification(previousOwner, NotificationType.SURVEY_OWNERSHIP_TRANSFERRED, "Propiedad de encuesta transferida", "Has transferido la propiedad de " + survey.getTitle(), survey);
    }

    public UserResponseDto toResponse(User user) {
        return new UserResponseDto(user.getId(), user.getRole(), user.getEmail(), user.getName(), user.getGender(), user.getInstitution(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private PageRequest createPageRequest(int page, int size) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size);
    }

    private boolean updateProfile(User user, UpdateAccountRequestDto.ProfileData profile) {
        String name = requireText(profile.name(), "Introduce tu nombre completo");
        String institution = requireText(profile.institution(), "Introduce tu institución");

        if (profile.gender() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un género válido");
        }

        boolean changed = false;

        if (!name.equals(user.getName())) {
            user.setName(name);
            changed = true;
        }
        if (!institution.equals(user.getInstitution())) {
            user.setInstitution(institution);
            changed = true;
        }
        if (profile.gender() != user.getGender()) {
            user.setGender(profile.gender());
            changed = true;
        }
        return changed;
    }

    private boolean updatePassword(User user, UpdateAccountRequestDto.PasswordData password) {
        String newPassword = requireText(password.newPassword(), "Introduce la nueva contraseña");

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return true;
    }

    private boolean updateEmail(User user, UpdateAccountRequestDto.EmailData email) {
        String newEmail = EmailNormalizer.normalize(requireText(email.newEmail(), "Introduce el nuevo correo electrónico"));

        if (newEmail.equals(user.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nuevo correo debe ser distinto al actual");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico");
        }

        user.setEmail(newEmail);
        return true;
    }

    private void ensureCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Introduce tu contraseña actual");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La contraseña actual no es correcta");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private void ensureAnotherSuperadmin(Long currentSuperadminId) {
        if (userRepository.countByRoleAndIdNot(UserRole.SUPERADMIN, currentSuperadminId) < 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Debe existir siempre un superadministrador");
        }
    }
}
