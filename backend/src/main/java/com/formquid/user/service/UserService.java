package com.formquid.user.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyCollaboratorRepository collaboratorRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        return userRepository.findAllResponses();
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(User user) {
        return userRepository.findResponseById(user.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
            );
    }

    /**
     * Recibe usuario y nuevo rol, devuelve el usuario actualizado.
     */
    @Transactional
    public UserResponseDto updateRole(UUID id, UserRole newRole) {
        User user = getById(id);
        if (user.getRole() == UserRole.SUPERADMIN && newRole != UserRole.SUPERADMIN) {
            ensureAnotherSuperadmin(user.getId());
        }
        if (newRole == UserRole.SUPERADMIN && userRepository.countByRole(UserRole.SUPERADMIN) >= 1 && user.getRole() != UserRole.SUPERADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo puede existir un superadministrador");
        }
        user.setRole(newRole);
        return toResponse(user);
    }

    /**
     * Recibe el usuario a borrar. No devuelve contenido.
     */
    @Transactional
    public void delete(UUID id) {
        User user = getById(id);
        if (surveyRepository.countByOwnerId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No puede eliminarse un propietario de encuestas");
        }
        if (user.getRole() == UserRole.SUPERADMIN) {
            ensureAnotherSuperadmin(id);
        }
        userRepository.delete(user);
    }

    /**
     * Recibe encuesta y nuevo propietario, devuelve la encuesta transferida.
     */
    @Transactional
    public void transferSurveyOwnership(String surveyId, UUID currentOwnerId, UUID newOwnerId) {
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
        notificationService.createSurveyNotification(
            newOwner,
            NotificationType.SURVEY_OWNERSHIP_TRANSFERRED,
            "Propiedad de encuesta transferida",
            "Ahora eres propietario de la encuesta " + survey.getTitle(),
            survey
        );
        notificationService.createSurveyNotification(
            previousOwner,
            NotificationType.SURVEY_OWNERSHIP_TRANSFERRED,
            "Propiedad de encuesta transferida",
            "Has transferido la propiedad de " + survey.getTitle(),
            survey
        );
    }

    private void ensureAnotherSuperadmin(UUID currentSuperadminId) {
        if (userRepository.countByRoleAndIdNot(UserRole.SUPERADMIN, currentSuperadminId) < 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Debe existir siempre un superadministrador");
        }
    }

    public UserResponseDto toResponse(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getRole(),
            user.getEmail(),
            user.getName(),
            user.getGender(),
            user.getInstitution(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
