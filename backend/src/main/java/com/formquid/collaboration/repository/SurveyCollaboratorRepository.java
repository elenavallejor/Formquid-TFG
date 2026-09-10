package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyCollaboratorRepository extends JpaRepository<SurveyCollaborator, SurveyCollaboratorId> {

    boolean existsByIdSurveyIdAndIdUserId(Long surveyId, Long userId);

    List<SurveyCollaborator> findByIdUserId(Long userId);

    List<SurveyCollaborator> findByIdSurveyId(Long surveyId);

    void deleteByIdSurveyIdAndIdUserId(Long surveyId, Long userId);
}