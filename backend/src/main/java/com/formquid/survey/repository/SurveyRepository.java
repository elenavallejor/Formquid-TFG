package com.formquid.survey.repository;

import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accede a las encuestas y concentra las consultas optimizadas de listados y administración.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    long countByOwnerId(Long ownerId);

    Optional<Survey> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);

    List<Survey> findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(
        SurveyLifecycle lifecycleStatus,
        SurveyVisibility visibility
    );

    @Query("""
        select new com.formquid.survey.dto.AdminSurveySummaryDto(
            survey.id,
            survey.title,
            survey.owner.name,
            survey.lifecycleStatus,
            survey.accessMode,
            survey.visibility,
            count(participation.id),
            survey.updatedAt
        )
        from Survey survey
        left join Participation participation
            on participation.survey = survey
        group by survey.id,
            survey.title,
            survey.owner.name,
            survey.lifecycleStatus,
            survey.accessMode,
            survey.visibility,
            survey.updatedAt
        order by survey.updatedAt desc
    """)
    List<AdminSurveySummaryDto> findAdminSummaries();

    @Query("""
        select new com.formquid.survey.dto.AdminSurveySummaryDto(
            survey.id,
            survey.title,
            survey.owner.name,
            survey.lifecycleStatus,
            survey.accessMode,
            survey.visibility,
            count(participation.id),
            survey.updatedAt
        )
        from Survey survey
        left join Participation participation
            on participation.survey = survey
        group by survey.id,
            survey.title,
            survey.owner.name,
            survey.lifecycleStatus,
            survey.accessMode,
            survey.visibility,
            survey.updatedAt
        order by survey.updatedAt desc
    """)
    List<AdminSurveySummaryDto> findAdminSummaries(Pageable pageable);

    @Query(
        value = """
            select survey
            from Survey survey
            where (
                survey.owner.id = :userId
                or exists (
                    select 1
                    from SurveyCollaborator collaborator
                    where collaborator.survey = survey
                        and collaborator.user.id = :userId
                )
            )
                and (:search = '' or lower(survey.title) like concat('%', :search, '%'))
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
        """,
        countQuery = """
            select count(survey.id)
            from Survey survey
            where (
                survey.owner.id = :userId
                or exists (
                    select 1
                    from SurveyCollaborator collaborator
                    where collaborator.survey = survey
                        and collaborator.user.id = :userId
                )
            )
                and (:search = '' or lower(survey.title) like concat('%', :search, '%'))
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
        """
    )
    Page<Survey> findEditableByUser(
        @Param("userId") Long userId,
        @Param("search") String search,
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        Pageable pageable
    );
}