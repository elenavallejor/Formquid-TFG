package com.formquid.registration.service;

import com.formquid.common.exception.ApiException;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.registration.dto.CreateRegistrationRequestDto;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /**
     * Devuelve todas las solicitudes pendientes ordenadas por fecha.
     */
    @Transactional(readOnly = true)
    public List<RegistrationRequestResponseDto> findPending() {
        return requestRepository.findAllResponses();
    }

    /**
     * Crea una nueva solicitud de registro.
     */
    @Transactional
    public RegistrationRequestResponseDto create(CreateRegistrationRequestDto request) {
        String email = EmailNormalizer.normalize(request.email());

        if (userRepository.existsByEmail(email) || requestRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado o pendiente de aprobación");
        }

        RegistrationRequest entity = RegistrationRequest.builder()
            .email(email)
            .name(request.name().trim())
            .gender(request.gender())
            .institution(request.institution().trim())
            .motivation(request.motivation().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

        RegistrationRequest saved = requestRepository.save(entity);

        notificationService.notifyAdminsAboutRegistrationRequest(saved);

        return toResponse(saved);
    }

    /**
     * Aprueba una solicitud, crea el usuario y elimina la solicitud.
     */
    @Transactional
    public UUID approve(UUID requestId) {

        RegistrationRequest request = getRequest(requestId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "Ya existe un usuario con ese correo"
            );
        }

        User user = User.builder()
            .role(UserRole.EDITOR)
            .email(request.getEmail())
            .name(request.getName())
            .gender(request.getGender())
            .institution(request.getInstitution())
            .passwordHash(request.getPasswordHash())
            .build();

        User saved = userRepository.save(user);

        /*
         * Se registra el correo de aprobación.
         */
        notificationService.createEmail(
            saved,
            NotificationType.REGISTRATION_APPROVED,
            "Solicitud aprobada",
            "Tu solicitud de registro ha sido aprobada. "
                + "Tu cuenta de FormQuid ya está activa."
        );

        /*
         * También se crea una notificación interna para que el usuario
         * pueda verla cuando acceda a la plataforma.
         *
         * El target es el propio usuario, que seguirá existiendo.
         */
        notificationService.createUserNotification(
            saved,
            NotificationType.REGISTRATION_APPROVED,
            "Cuenta aprobada",
            "Tu cuenta de FormQuid ya está activa.",
            saved
        );

        /*
         * Las notificaciones que apuntan a la solicitud dejan de ser útiles
         * cuando esta se resuelve.
         */
        notificationService.deleteRegistrationRequestNotifications(
            request.getId()
        );

        /*
         * Una solicitud solo existe mientras está pendiente.
         */
        requestRepository.delete(request);

        return saved.getId();
    }

    /**
     * Rechaza y elimina una solicitud de registro.
     */
    @Transactional
    public void reject(UUID requestId) {

        RegistrationRequest request = getRequest(requestId);

        /*
         * TODO IMPLEMENTAR CORREO.
         *
         * El correo de rechazo deberá enviarse mediante el servicio de
         * correo externo antes de eliminar la solicitud.
         *
         * Ejemplo futuro:
         *
         * emailService.sendRegistrationRejected(
         *     request.getEmail(),
         *     request.getName()
         * );
         */

        notificationService.deleteRegistrationRequestNotifications(
            request.getId()
        );

        requestRepository.delete(request);
    }

    /**
     * Obtiene una solicitud pendiente.
     *
     * Si no existe, significa que ya fue aprobada, rechazada
     * o que el identificador es incorrecto.
     */
    private RegistrationRequest getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "Solicitud no encontrada"
            ));
    }

    /**
     * Convierte una solicitud a su DTO de respuesta.
     */
    private RegistrationRequestResponseDto toResponse(
        RegistrationRequest request
    ) {
        return new RegistrationRequestResponseDto(
            request.getId(),
            request.getEmail(),
            request.getName(),
            request.getGender(),
            request.getInstitution(),
            request.getMotivation(),
            request.getCreatedAt()
        );
    }
}