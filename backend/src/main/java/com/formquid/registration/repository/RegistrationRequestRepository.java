package com.formquid.registration.repository;

import com.formquid.common.entity.RequestStatus;
import com.formquid.registration.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, UUID> {

    boolean existsByEmailAndStatus(String email, RequestStatus status);

    List<RegistrationRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
}
