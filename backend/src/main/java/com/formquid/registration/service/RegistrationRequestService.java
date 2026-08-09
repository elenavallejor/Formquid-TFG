package com.formquid.registration.service;

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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de gestionar las solicitudes de registro de editores.
 */
@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea una solicitud pública de registro como editor.
     *
     * @param request datos introducidos por el solicitante.
     * @return solicitud creada.
     */
    @Transactional
    public RegistrationRequestResponseDto create(CreateRegistrationRequestDto request) {
        validateNewRequest(request.email());

        RegistrationRequest registrationRequest = RegistrationRequest.builder()
            .fullName(request.fullName())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .gender(request.gender())
            .institution(request.institution())
            .motivation(request.motivation())
            .status(RegistrationRequestStatus.PENDING)
            .build();

        return mapToResponseDto(registrationRequestRepository.save(registrationRequest));
    }

    /**
     * Devuelve todas las solicitudes registradas.
     *
     * @return lista de solicitudes.
     */
    @Transactional(readOnly = true)
    public List<RegistrationRequestResponseDto> findAll() {
        return registrationRequestRepository.findAll()
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve únicamente las solicitudes pendientes.
     *
     * @return lista de solicitudes pendientes.
     */
    @Transactional(readOnly = true)
    public List<RegistrationRequestResponseDto> findPending() {
        return registrationRequestRepository
            .findByStatusOrderByRequestedAtAsc(RegistrationRequestStatus.PENDING)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Aprueba una solicitud y crea el usuario editor correspondiente.
     *
     * @param requestId identificador de la solicitud.
     * @param resolverId identificador del administrador que resuelve la solicitud.
     * @return solicitud aprobada.
     */
    @Transactional
    public RegistrationRequestResponseDto approve(Long requestId, Long resolverId) {
        RegistrationRequest request = findEntityById(requestId);
        User resolver = findUserById(resolverId);

        ensureRequestIsPending(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe un usuario registrado con ese correo electrónico"
            );
        }

        User editor = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(request.getPasswordHash())
            .role(UserRole.EDITOR)
            .gender(request.getGender())
            .institution(request.getInstitution())
            .enabled(true)
            .build();

        userRepository.save(editor);

        request.setStatus(RegistrationRequestStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(resolver);
        request.setRejectionReason(null);

        return mapToResponseDto(registrationRequestRepository.save(request));
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * @param requestId identificador de la solicitud.
     * @param resolverId identificador del administrador que resuelve la solicitud.
     * @param request datos opcionales de resolución.
     * @return solicitud rechazada.
     */
    @Transactional
    public RegistrationRequestResponseDto reject(
        Long requestId,
        Long resolverId,
        ResolveRegistrationRequestDto request
    ) {
        RegistrationRequest registrationRequest = findEntityById(requestId);
        User resolver = findUserById(resolverId);

        ensureRequestIsPending(registrationRequest);

        registrationRequest.setStatus(RegistrationRequestStatus.REJECTED);
        registrationRequest.setResolvedAt(LocalDateTime.now());
        registrationRequest.setResolvedBy(resolver);
        registrationRequest.setRejectionReason(
            request != null ? request.rejectionReason() : null
        );

        return mapToResponseDto(registrationRequestRepository.save(registrationRequest));
    }

    private void validateNewRequest(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe un usuario registrado con ese correo electrónico"
            );
        }

        boolean hasPendingRequest = registrationRequestRepository.existsByEmailAndStatus(
            email,
            RegistrationRequestStatus.PENDING
        );

        if (hasPendingRequest) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe una solicitud pendiente con ese correo electrónico"
            );
        }
    }

    private RegistrationRequest findEntityById(Long id) {
        return registrationRequestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Solicitud de registro no encontrada"
            ));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));
    }

    private void ensureRequestIsPending(RegistrationRequest request) {
        if (request.getStatus() != RegistrationRequestStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud ya ha sido resuelta"
            );
        }
    }

    private RegistrationRequestResponseDto mapToResponseDto(RegistrationRequest request) {
        Long resolvedByUserId = request.getResolvedBy() != null
            ? request.getResolvedBy().getId()
            : null;

        return new RegistrationRequestResponseDto(
            request.getId(),
            request.getFullName(),
            request.getEmail(),
            request.getGender(),
            request.getInstitution(),
            request.getMotivation(),
            request.getStatus(),
            request.getRequestedAt(),
            request.getResolvedAt(),
            resolvedByUserId,
            request.getRejectionReason()
        );
    }
}