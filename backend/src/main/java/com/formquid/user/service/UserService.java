package com.formquid.user.service;

import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.user.dto.UpdateUserRoleRequestDto;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.dto.UserSummaryResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de negocio para la gestión y consulta de usuarios.
 *
 * Centraliza tanto las operaciones administrativas sobre cuentas
 * como las consultas generales de usuarios.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Lista todos los usuarios registrados.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        return userRepository.findAllByOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lista usuarios filtrados por rol.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> findByRole(UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El rol indicado no es válido"
            );
        }

        return userRepository.findByRoleOrderByNameAsc(role)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Busca editores activos por nombre, correo o institución.
     *
     * Se excluye al propio usuario autenticado de los resultados.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryResponseDto> searchEditors(
        String query,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);

        String normalizedQuery = normalizeQuery(query);

        return userRepository.searchEnabledUsersByRole(
                UserRole.EDITOR,
                normalizedQuery,
                authenticatedUser.getId()
            )
            .stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Activa una cuenta.
     *
     * Un ADMIN solo puede modificar cuentas EDITOR.
     * Un SUPERADMIN puede modificar cuentas de cualquier rol.
     */
    @Transactional
    public UserResponseDto enableUser(
        Long targetUserId,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        User targetUser = getUser(targetUserId);

        validateCanModifyAccount(
            targetUser,
            authenticatedUser
        );

        targetUser.setEnabled(true);

        notifyAccountUpdated(
            targetUser,
            "Cuenta activada",
            "Tu cuenta de FormQuid ha sido activada."
        );

        return toResponse(targetUser);
    }

    /**
     * Desactiva una cuenta.
     *
     * No se permite desactivar la propia cuenta ni dejar el sistema
     * sin ningún SUPERADMIN activo.
     */
    @Transactional
    public UserResponseDto disableUser(
        Long targetUserId,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        User targetUser = getUser(targetUserId);

        validateCanModifyAccount(
            targetUser,
            authenticatedUser
        );

        validateNotSelfOperation(
            targetUser,
            authenticatedUser
        );

        validateNotLastEnabledSuperadmin(targetUser);

        targetUser.setEnabled(false);

        notifyAccountUpdated(
            targetUser,
            "Cuenta desactivada",
            "Tu cuenta de FormQuid ha sido desactivada por administración."
        );

        return toResponse(targetUser);
    }

    /**
     * Cambia el rol de un usuario.
     *
     * Solo un SUPERADMIN puede realizar esta operación.
     */
    @Transactional
    public UserResponseDto updateRole(
        Long targetUserId,
        UpdateUserRoleRequestDto request,
        String authenticatedEmail
    ) {
        User authenticatedUser = getAuthenticatedUser(authenticatedEmail);
        User targetUser = getUser(targetUserId);

        validateSuperadmin(authenticatedUser);

        validateNotSelfOperation(
            targetUser,
            authenticatedUser
        );

        validateRoleRequest(request);

        UserRole previousRole = targetUser.getRole();
        UserRole newRole = request.role();

        if (previousRole == newRole) {
            return toResponse(targetUser);
        }

        /*
         * Solo es necesario comprobar el último superadministrador
         * si realmente se le va a retirar ese rol.
         */
        if (previousRole == UserRole.SUPERADMIN
            && newRole != UserRole.SUPERADMIN) {
            validateNotLastEnabledSuperadmin(targetUser);
        }

        targetUser.setRole(newRole);

        notifyAccountUpdated(
            targetUser,
            "Rol actualizado",
            "Tu rol en FormQuid ha cambiado de "
                + previousRole
                + " a "
                + newRole
                + "."
        );

        return toResponse(targetUser);
    }

    /**
     * Obtiene un usuario por identificador.
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe el usuario indicado"
            ));
    }

    /**
     * Obtiene el usuario actualmente autenticado a partir de su correo.
     */
    private User getAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se ha podido identificar al usuario autenticado"
            ));
    }

    /**
     * Comprueba si el usuario autenticado puede modificar
     * la cuenta indicada.
     */
    private void validateCanModifyAccount(
        User targetUser,
        User authenticatedUser
    ) {
        UserRole authenticatedRole = authenticatedUser.getRole();

        if (authenticatedRole == UserRole.SUPERADMIN) {
            return;
        }

        if (authenticatedRole == UserRole.ADMIN
            && targetUser.getRole() == UserRole.EDITOR) {
            return;
        }

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No tienes permisos para modificar esta cuenta"
        );
    }

    /**
     * Comprueba que el usuario autenticado sea SUPERADMIN.
     */
    private void validateSuperadmin(User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.SUPERADMIN) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo un superadministrador puede cambiar roles de usuario"
            );
        }
    }

    /**
     * Evita operaciones críticas sobre la propia cuenta.
     */
    private void validateNotSelfOperation(
        User targetUser,
        User authenticatedUser
    ) {
        if (targetUser.getId().equals(authenticatedUser.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No puedes realizar esta operación sobre tu propia cuenta"
            );
        }
    }

    /**
     * Evita dejar el sistema sin ningún SUPERADMIN activo.
     */
    private void validateNotLastEnabledSuperadmin(User targetUser) {
        if (targetUser.getRole() != UserRole.SUPERADMIN
            || !targetUser.isEnabled()) {
            return;
        }

        long enabledSuperadmins = userRepository.countByRoleAndEnabled(
            UserRole.SUPERADMIN,
            true
        );

        if (enabledSuperadmins <= 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede modificar el último superadministrador activo"
            );
        }
    }

    /**
     * Valida el DTO utilizado para cambiar el rol.
     */
    private void validateRoleRequest(
        UpdateUserRoleRequestDto request
    ) {
        if (request == null || request.role() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El nuevo rol es obligatorio"
            );
        }
    }

    /**
     * Normaliza el texto utilizado en las búsquedas.
     */
    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        return query.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Genera una notificación interna cuando cambia una cuenta.
     */
    private void notifyAccountUpdated(
        User user,
        String title,
        String message
    ) {
        notificationService.notifyInternal(
            user.getId(),
            NotificationType.ACCOUNT_UPDATED,
            title,
            message,
            NotificationTargetType.USER,
            user.getId()
        );
    }

    /**
     * Convierte User al DTO utilizado en administración.
     */
    private UserResponseDto toResponse(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getGender(),
            user.getInstitution(),
            user.isEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * Convierte User a su representación resumida.
     */
    private UserSummaryResponseDto toSummary(User user) {
        return new UserSummaryResponseDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getInstitution()
        );
    }
}