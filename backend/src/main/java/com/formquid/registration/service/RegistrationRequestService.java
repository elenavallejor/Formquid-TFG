package com.formquid.registration.service;

import com.formquid.common.exception.ApiException;
import com.formquid.common.entity.RequestStatus;
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

    public List<RegistrationRequestResponseDto> findPending() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream().map(this::toResponse).toList();
    }

    /**
     * Recibe datos de registro y devuelve la solicitud pendiente.
     */
    @Transactional
    public RegistrationRequestResponseDto create(CreateRegistrationRequestDto request) {
        String email = EmailNormalizer.normalize(request.email());
        if (userRepository.existsByEmail(email) || requestRepository.existsByEmailAndStatus(email, RequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado o pendiente de aprobación");
        }

        RegistrationRequest entity = RegistrationRequest.builder()
            .email(email)
            .name(request.name().trim())
            .gender(request.gender())
            .institution(request.institution().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();
        RegistrationRequest saved = requestRepository.save(entity);
        notifyAdmins(saved);
        return toResponse(saved);
    }

    /**
     * Recibe una solicitud pendiente y devuelve el usuario creado.
     */
    @Transactional
    public UUID approve(UUID requestId) {
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
        notificationService.createRegistrationApprovedEmail(saved, request);
        notificationService.createUserNotification(
            saved,
            NotificationType.REGISTRATION_APPROVED,
            "Cuenta aprobada",
            "Tu cuenta de FormQuid ya está activa.",
            saved
        );
        request.approve();
        return saved.getId();
    }

    /**
     * Recibe una solicitud pendiente. No devuelve contenido.
     */
    @Transactional
    public void reject(UUID requestId) {
        RegistrationRequest request = getRequest(requestId);
        // Con notifications simplificada no se persiste email sin usuario.
        // El proveedor de correo debe enviar el rechazo usando request.getEmail().
        request.reject();
    }

    private RegistrationRequest getRequest(UUID requestId) {
        RegistrationRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud ya está resuelta");
        }
        return request;
    }

    private void notifyAdmins(RegistrationRequest request) {
        userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPERADMIN)
            .forEach(user -> notificationService.createRegistrationRequestNotification(
                user,
                NotificationType.REGISTRATION_REQUEST,
                "Nueva solicitud de registro",
                "Hay una nueva solicitud de cuenta para " + request.getEmail(),
                request
            ));
    }

    private RegistrationRequestResponseDto toResponse(RegistrationRequest request) {
        return new RegistrationRequestResponseDto(
            request.getId(),
            request.getEmail(),
            request.getName(),
            request.getGender(),
            request.getInstitution(),
            request.getStatus(),
            request.getCreatedAt(),
            request.getResolvedAt()
        );
    }
}
