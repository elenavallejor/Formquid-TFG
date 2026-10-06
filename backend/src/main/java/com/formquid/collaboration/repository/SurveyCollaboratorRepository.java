package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.entity.CollaborationPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyCollaboratorRepository extends JpaRepository<SurveyCollaborator, SurveyCollaboratorId> {

    boolean existsByIdSurveyIdAndIdUserId(Long surveyId, Long userId);

    boolean existsByIdSurveyIdAndIdUserIdAndPermission(Long surveyId, Long userId, CollaborationPermission permission);

    @Query("""
        select collaborator.permission
        from SurveyCollaborator collaborator
        where collaborator.id.surveyId = :surveyId and collaborator.id.userId = :userId
        """)
    Optional<CollaborationPermission> findPermission(@Param("surveyId") Long surveyId, @Param("userId") Long userId);

    List<SurveyCollaborator> findByIdSurveyId(Long surveyId);

    @Query("""
        select collaborator.id.surveyId
        from SurveyCollaborator collaborator
        where collaborator.id.userId = :userId
            and collaborator.id.surveyId in :surveyIds
        """)
    List<Long> findSurveyIdsByUserIdAndSurveyIds(
        @Param("userId") Long userId,
        @Param("surveyIds") List<Long> surveyIds
    );

    void deleteByIdSurveyIdAndIdUserId(Long surveyId, Long userId);
}
