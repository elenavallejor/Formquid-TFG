package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.entity.CollaborationPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyCollaboratorRepository extends JpaRepository<SurveyCollaborator, SurveyCollaboratorId> {

    boolean existsByIdSurveyIdAndIdUserId(Long surveyId, Long userId);

    boolean existsByIdSurveyIdAndIdUserIdAndPermission(Long surveyId, Long userId, CollaborationPermission permission);

    List<SurveyCollaborator> findByIdUserId(Long userId);

    List<SurveyCollaborator> findByIdSurveyId(Long surveyId);

    void deleteByIdSurveyIdAndIdUserId(Long surveyId, Long userId);
}
