package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SurveyCollaboratorRepository extends JpaRepository<SurveyCollaborator, SurveyCollaboratorId> {

    boolean existsByIdSurveyIdAndIdUserId(String surveyId, UUID userId);

    List<SurveyCollaborator> findByIdUserId(UUID userId);

    List<SurveyCollaborator> findByIdSurveyId(String surveyId);

    void deleteByIdSurveyIdAndIdUserId(String surveyId, UUID userId);
}
