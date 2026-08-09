package com.formquid.registration.repository;

import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.entity.RegistrationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para solicitudes de registro.
 */
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {

    /**
     * Comprueba si existe una solicitud con un correo y estado concretos.
     *
     * @param email correo electrónico del solicitante.
     * @param status estado de la solicitud.
     * @return true si existe una solicitud coincidente.
     */
    boolean existsByEmailAndStatus(String email, RegistrationRequestStatus status);

    /**
     * Obtiene las solicitudes con un estado concreto ordenadas por fecha.
     *
     * @param status estado de las solicitudes.
     * @return lista de solicitudes encontradas.
     */
    List<RegistrationRequest> findByStatusOrderByRequestedAtAsc(RegistrationRequestStatus status);
}