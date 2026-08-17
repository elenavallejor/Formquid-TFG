package com.formquid.registration.service;

import com.formquid.notification.service.NotificationService;
import com.formquid.registration.dto.CreateRegistrationRequestDto;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.dto.ResolveRegistrationRequestDto;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.entity.RegistrationRequestStatus;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Servicio encargado de gestionar las solicitudes de registro de editores.
 *
 * Una persona externa puede crear una solicitud sin disponer todavía de
 * cuenta. La cuenta EDITOR se crea únicamente cuando un administrador o
 * superadministrador aprueba la solicitud.
 *
 * Las consultas son de solo lectura por defecto. Las operaciones create,
 * approve y reject abren transacciones de escritura explícitas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationRequestService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /**
     * Crea una solicitud pública de registro.
     *
     * El correo se normaliza para evitar duplicados provocados por
     * diferencias de mayúsculas, minúsculas o espacios.
     *
     * La contraseña se cifra antes de almacenar la solicitud y nunca
     * permanece en texto plano en la base de datos.
     */
    @Transactional
    public RegistrationRequestResponseDto create(
        CreateRegistrationRequestDto request
    ) {
        String email = normalizeEmail(
            request.email()
        );

        validateNewRequest(email);

        RegistrationRequest registrationRequest =
            RegistrationRequest.builder()
                .name(request.name().trim())
                .email(email)
                .password(
                    passwordEncoder.encode(
                        request.password()
                    )
                )
                .gender(request.gender())
                .institution(
                    request.institution().trim()
                )
                .motivation(
                    request.motivation().trim()
                )
                .status(
                    RegistrationRequestStatus.PENDING
                )
                .build();

        return toResponse(
            registrationRequestRepository.save(
                registrationRequest
            )
        );
    }

    /**
     * Obtiene todas las solicitudes de registro.
     */
    public List<RegistrationRequestResponseDto> getAll() {
        return registrationRequestRepository
            .findAllByOrderByRequestedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene únicamente las solicitudes pendientes.
     */
    public List<RegistrationRequestResponseDto> getPending() {
        return registrationRequestRepository
            .findByStatusOrderByRequestedAtAsc(
                RegistrationRequestStatus.PENDING
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Devuelve el número de solicitudes todavía pendientes.
     *
     * El recuento se ejecuta directamente en la base de datos.
     */
    public long getPendingCount() {
        return registrationRequestRepository
            .countByStatus(
                RegistrationRequestStatus.PENDING
            );
    }

    /**
     * Elimina las solicitudes que llevan al menos siete días resueltas.
     *
     * La antigüedad se calcula desde resolvedAt, no desde requestedAt, ya que
     * el periodo de conservación comienza cuando la solicitud se aprueba o
     * rechaza. Las solicitudes PENDING nunca son eliminadas por esta tarea.
     *
     * @return número de solicitudes eliminadas.
     */
    @Transactional
    public int deleteExpiredResolvedRequests() {
        LocalDateTime expirationDate =
            LocalDateTime.now().minusDays(7);

        return registrationRequestRepository
            .deleteResolvedBefore(
                RegistrationRequestStatus.APPROVED,
                RegistrationRequestStatus.REJECTED,
                expirationDate
            );
    }

    /**
     * Aprueba una solicitud y crea el usuario EDITOR correspondiente.
     *
     * La creación del usuario y la resolución de RegistrationRequest forman
     * parte de la misma transacción. Si cualquiera de las operaciones falla,
     * Spring realiza rollback y no queda información parcialmente persistida.
     *
     * La contraseña almacenada en RegistrationRequest ya está cifrada, por
     * lo que se copia directamente al nuevo usuario y no vuelve a codificarse.
     */
    @Transactional
    public RegistrationRequestResponseDto approve(
        Long requestId,
        String adminEmail
    ) {
        RegistrationRequest request =
            getRequest(requestId);

        User admin =
            getAdministrator(adminEmail);

        /*
         * La entidad valida aquí que la solicitud continúe pendiente.
         * Si ya estaba resuelta, no se llega a crear ningún usuario.
         */
        request.approve(admin);

        /*
         * Se comprueba cualquier usuario existente, esté activo o no,
         * porque la columna users.email es única en la base de datos.
         */
        if (
            userRepository
                .findByEmailIgnoreCase(
                    request.getEmail()
                )
                .isPresent()
        ) {
            throw new IllegalArgumentException(
                "Ya existe un usuario registrado con ese correo electrónico"
            );
        }

        User editor =
            User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(UserRole.EDITOR)
                .gender(request.getGender())
                .institution(request.getInstitution())
                .enabled(true)
                .build();

        User savedEditor =
            userRepository.save(editor);

        /*
         * RegistrationRequest es una entidad gestionada por JPA.
         * No es necesario ejecutar save(request): dirty checking persistirá
         * el cambio de estado, resolvedBy y resolvedAt al confirmar la
         * transacción.
         */
        notificationService
            .notifyRegistrationApproved(
                savedEditor.getId(),
                request.getId()
            );

        return toResponse(request);
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * No se crea una notificación interna porque el solicitante todavía
     * no dispone de una cuenta de usuario en FormQuid.
     */
    @Transactional
    public RegistrationRequestResponseDto reject(
        Long requestId,
        ResolveRegistrationRequestDto resolution,
        String adminEmail
    ) {
        RegistrationRequest request =
            getRequest(requestId);

        User admin =
            getAdministrator(adminEmail);

        request.reject(
            admin,
            resolveRejectionReason(
                resolution
            )
        );

        /*
         * La entidad está gestionada y la actualización se persiste mediante
         * dirty checking cuando finaliza la transacción.
         */
        return toResponse(request);
    }

    /**
     * Impide registrar una nueva solicitud si ya existe un usuario
     * con ese correo o si existe otra solicitud pendiente.
     */
    private void validateNewRequest(
        String email
    ) {
        if (
            userRepository
                .findByEmailIgnoreCase(email)
                .isPresent()
        ) {
            throw new IllegalArgumentException(
                "Ya existe un usuario registrado con ese correo electrónico"
            );
        }

        boolean hasPendingRequest =
            registrationRequestRepository
                .existsByEmailIgnoreCaseAndStatus(
                    email,
                    RegistrationRequestStatus.PENDING
                );

        if (hasPendingRequest) {
            throw new IllegalArgumentException(
                "Ya existe una solicitud pendiente con ese correo electrónico"
            );
        }
    }

    /**
     * Obtiene una solicitud o devuelve HTTP 404.
     */
    private RegistrationRequest getRequest(
        Long requestId
    ) {
        return registrationRequestRepository
            .findById(requestId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Solicitud de registro no encontrada"
                )
            );
    }

    /**
     * Obtiene el administrador autenticado mediante su correo.
     *
     * La comprobación del rol se mantiene también en servicio como defensa
     * adicional, aunque los endpoints administrativos estén protegidos con
     * @PreAuthorize.
     */
    private User getAdministrator(
        String email
    ) {
        User user =
            getAuthenticatedUser(email);

        if (
            user.getRole() != UserRole.ADMIN
            && user.getRole() != UserRole.SUPERADMIN
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo un administrador puede resolver solicitudes de registro"
            );
        }

        return user;
    }

    /**
     * Recupera al usuario autenticado a partir del correo que Spring Security
     * expone mediante java.security.Principal.
     */
    private User getAuthenticatedUser(
        String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(
                email.trim()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Obtiene de forma segura el motivo de rechazo opcional.
     */
    private String resolveRejectionReason(
        ResolveRegistrationRequestDto resolution
    ) {
        if (
            resolution == null
            || !StringUtils.hasText(
                resolution.rejectionReason()
            )
        ) {
            return null;
        }

        return resolution
            .rejectionReason()
            .trim();
    }

    /**
     * Normaliza un correo antes de compararlo y almacenarlo.
     */
    private String normalizeEmail(
        String email
    ) {
        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    /**
     * Convierte una entidad de solicitud en su DTO de salida.
     *
     * La contraseña cifrada nunca se expone al cliente.
     */
    private RegistrationRequestResponseDto toResponse(
        RegistrationRequest request
    ) {
        User resolvedBy =
            request.getResolvedBy();

        return new RegistrationRequestResponseDto(
            request.getId(),
            request.getName(),
            request.getEmail(),
            request.getGender(),
            request.getInstitution(),
            request.getMotivation(),
            request.getStatus(),
            request.getRequestedAt(),
            request.getResolvedAt(),
            resolvedBy != null
                ? resolvedBy.getId()
                : null,
            resolvedBy != null
                ? resolvedBy.getName()
                : null,
            request.getRejectionReason()
        );
    }
}
