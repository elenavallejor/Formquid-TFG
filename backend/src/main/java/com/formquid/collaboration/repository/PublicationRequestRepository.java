package com.formquid.collaboration.repository;

import com.formquid.common.entity.RequestStatus;
import com.formquid.collaboration.entity.PublicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublicationRequestRepository extends JpaRepository<PublicationRequest, UUID> {

    boolean existsBySurveyIdAndStatus(String surveyId, RequestStatus status);
}
