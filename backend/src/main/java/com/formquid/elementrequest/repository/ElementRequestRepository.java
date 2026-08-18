package com.formquid.elementrequest.repository;

import com.formquid.common.entity.RequestStatus;
import com.formquid.elementrequest.entity.ElementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;

public interface ElementRequestRepository extends JpaRepository<ElementRequest, UUID> {

    List<ElementRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
}
