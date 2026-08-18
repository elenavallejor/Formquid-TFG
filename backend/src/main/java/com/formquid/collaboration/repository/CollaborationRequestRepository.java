package com.formquid.collaboration.repository;

import com.formquid.common.entity.RequestStatus;
import com.formquid.collaboration.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, UUID> {

    boolean existsBySurveyIdAndUserIdAndStatus(String surveyId, UUID userId, RequestStatus status);

    List<CollaborationRequest> findBySurveyIdAndStatusOrderByCreatedAtDesc(String surveyId, RequestStatus status);
}
