package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.PublicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicationRequestRepository
    extends JpaRepository<PublicationRequest, Long> {

    boolean existsBySurveyId(Long surveyId);

    List<PublicationRequest> findBySurveyIdOrderByCreatedAtDesc(Long surveyId);
}
