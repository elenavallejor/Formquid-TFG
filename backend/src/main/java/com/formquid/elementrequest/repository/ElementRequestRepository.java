package com.formquid.elementrequest.repository;

import com.formquid.elementrequest.entity.ElementRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ElementRequestRepository
    extends JpaRepository<ElementRequest, UUID> {

    List<ElementRequest> findAllByOrderByCreatedAtAsc();
}