package com.formquid.survey.repository;

import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, String> {

    List<Survey> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    List<Survey> findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(
        SurveyLifecycle lifecycleStatus,
        SurveyVisibility visibility
    );

    @Query("""
        select distinct survey
        from Survey survey
        left join SurveyCollaborator collaborator
            on collaborator.survey = survey
        where survey.owner.id = :userId
            or collaborator.user.id = :userId
        order by survey.createdAt desc
    """)
    List<Survey> findEditableByUser(@Param("userId") UUID userId);

    boolean existsByIdAndOwnerId(String id, UUID ownerId);
}
