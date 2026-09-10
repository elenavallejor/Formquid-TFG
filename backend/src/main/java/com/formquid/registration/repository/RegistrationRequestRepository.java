package com.formquid.registration.repository;

import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegistrationRequestRepository
    extends JpaRepository<RegistrationRequest, Long> {

    boolean existsByEmail(String email);

    /**
     * Proyección para administración. Excluye password_hash de la consulta.
     */
    @Query("""
        select new com.formquid.registration.dto.RegistrationRequestResponseDto(
            request.id,
            request.email,
            request.name,
            request.gender,
            request.institution,
            request.motivation,
            request.createdAt
        )
        from RegistrationRequest request
        order by request.createdAt desc
    """)
    List<RegistrationRequestResponseDto> findAllResponses();
}