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

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<RegistrationRequestResponseDto> findPending() {
        return requestRepository.findAllResponses();
    }

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
        notificationService.sendPriorityEmail(email, NotificationType.REGISTRATION_RECEIVED,
            "Solicitud de cuenta recibida",
            "Hemos recibido tu solicitud de cuenta en FormQuid, " + saved.getName()
                + ". Te avisaremos cuando se resuelva.");
        return toResponse(saved);
    }

    @Transactional
    public Long approve(Long requestId) {
        RegistrationRequest request = getRequest(requestId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
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

        notificationService.createUserNotification(
            saved,
            NotificationType.REGISTRATION_APPROVED,
            "Cuenta aprobada",
            "Tu cuenta de FormQuid ya está activa.",
            saved
        );

        requestRepository.delete(request);
        return saved.getId();
    }

    @Transactional
    public void reject(Long requestId) {
        RegistrationRequest request = getRequest(requestId);
        notificationService.sendPriorityEmail(request.getEmail(), NotificationType.REGISTRATION_REJECTED,
            "Solicitud de cuenta rechazada",
            "Tu solicitud de cuenta en FormQuid para " + request.getEmail() + " ha sido rechazada.");
        requestRepository.delete(request);
    }

    private RegistrationRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }

    private RegistrationRequestResponseDto toResponse(RegistrationRequest request) {
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
