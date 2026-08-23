package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, UUID> {

    boolean existsBySurveyIdAndUserId(String surveyId, UUID userId);
    List<CollaborationRequest> findBySurveyIdOrderByCreatedAtDesc(String surveyId);
}