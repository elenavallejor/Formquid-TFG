package com.formquid.registration.repository;

import com.formquid.registration.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationRequestRepository
    extends JpaRepository<RegistrationRequest, UUID> {

    boolean existsByEmail(String email);

    List<RegistrationRequest> findAllByOrderByCreatedAtAsc();
}