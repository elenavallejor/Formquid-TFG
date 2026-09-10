package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, Long> {

    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);
    List<CollaborationRequest> findBySurveyIdOrderByCreatedAtDesc(Long surveyId);
}