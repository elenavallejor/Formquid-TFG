package com.formquid.survey.repository;

import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.dto.SurveyResultsSummaryDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Accede a las encuestas y concentra las consultas optimizadas de listados y administración.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    long countByOwnerId(Long ownerId);

    Optional<Survey> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);

    @Query(
        value = """
            select survey
            from Survey survey
            where survey.lifecycleStatus = :lifecycleStatus
                and survey.visibility = :visibility
                and (:includeAuthenticated = true or survey.accessMode <> :authenticatedAccessMode)
                and (:accessMode is null or survey.accessMode = :accessMode)
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                )
            """,
        countQuery = """
            select count(survey.id)
            from Survey survey
            where survey.lifecycleStatus = :lifecycleStatus
                and survey.visibility = :visibility
                and (:includeAuthenticated = true or survey.accessMode <> :authenticatedAccessMode)
                and (:accessMode is null or survey.accessMode = :accessMode)
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                )
            """
    )
    Page<Survey> findListedOpenPage(
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        @Param("visibility") SurveyVisibility visibility,
        @Param("includeAuthenticated") boolean includeAuthenticated,
        @Param("authenticatedAccessMode") SurveyAccessMode authenticatedAccessMode,
        @Param("accessMode") SurveyAccessMode accessMode,
        @Param("search") String search,
        Pageable pageable
    );

    @Query(
        value = """
            select survey.*
            from surveys survey
            where survey.lifecycle_status = 'OPEN'
                and survey.visibility = 'LISTED'
                and (:includeAuthenticated = true or survey.access_mode <> 'AUTHENTICATED')
                and (:accessMode = '' or survey.access_mode = :accessMode)
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                )
                and exists (
                    select 1
                    from unnest(survey.tags) as survey_tag(tag_text)
                    where strpos(lower(survey_tag.tag_text), :tag) > 0
                )
            order by survey.created_at desc, survey.id desc
            """,
        countQuery = """
            select count(survey.id)
            from surveys survey
            where survey.lifecycle_status = 'OPEN'
                and survey.visibility = 'LISTED'
                and (:includeAuthenticated = true or survey.access_mode <> 'AUTHENTICATED')
                and (:accessMode = '' or survey.access_mode = :accessMode)
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                )
                and exists (
                    select 1
                    from unnest(survey.tags) as survey_tag(tag_text)
                    where strpos(lower(survey_tag.tag_text), :tag) > 0
                )
            """,
        nativeQuery = true
    )
    Page<Survey> findListedOpenPageByTag(
        @Param("includeAuthenticated") boolean includeAuthenticated,
        @Param("accessMode") String accessMode,
        @Param("search") String search,
        @Param("tag") String tag,
        Pageable pageable
    );

    @Query(
        value = """
            select new com.formquid.survey.dto.AdminSurveySummaryDto(
                survey.id,
                survey.title,
                survey.imageKey,
                survey.owner.name,
                survey.lifecycleStatus,
                survey.accessMode,
                survey.visibility,
                (
                    select count(participation.id)
                    from Participation participation
                    where participation.survey = survey
                ),
                survey.updatedAt
            )
            from Survey survey
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(survey.owner.name) like concat('%', :search, '%')
            )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
                and (:visibility is null or survey.visibility = :visibility)
            """,
        countQuery = """
            select count(survey.id)
            from Survey survey
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(survey.owner.name) like concat('%', :search, '%')
            )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
                and (:visibility is null or survey.visibility = :visibility)
            """
    )
    Page<AdminSurveySummaryDto> findAdminSummaries(
        @Param("search") String search,
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        @Param("visibility") SurveyVisibility visibility,
        Pageable pageable
    );

    @Query(
        value = """
            select new com.formquid.survey.dto.SurveyResultsSummaryDto(
                survey.id,
                survey.title,
                survey.imageKey,
                owner.name,
                survey.lifecycleStatus,
                count(participation.id),
                survey.updatedAt
            )
            from Participation participation
            join participation.survey survey
            join survey.owner owner
            where (
                :administrator = true
                or survey.owner.id = :userId
                or exists (
                    select 1
                    from SurveyCollaborator collaborator
                    where collaborator.survey = survey
                        and collaborator.user.id = :userId
                )
            )
                and (
                    :search = ''
                    or (:searchTitle = true and lower(survey.title) like concat('%', :search, '%'))
                    or (:searchOwner = true and lower(owner.name) like concat('%', :search, '%'))
                )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
            group by
                survey.id,
                survey.title,
                survey.imageKey,
                owner.name,
                survey.lifecycleStatus,
                survey.updatedAt
            """,
        countQuery = """
            select count(distinct survey.id)
            from Participation participation
            join participation.survey survey
            join survey.owner owner
            where (
                :administrator = true
                or survey.owner.id = :userId
                or exists (
                    select 1
                    from SurveyCollaborator collaborator
                    where collaborator.survey = survey
                        and collaborator.user.id = :userId
                )
            )
                and (
                    :search = ''
                    or (:searchTitle = true and lower(survey.title) like concat('%', :search, '%'))
                    or (:searchOwner = true and lower(owner.name) like concat('%', :search, '%'))
                )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
            """
    )
    Page<SurveyResultsSummaryDto> findResultSummaries(
        @Param("userId") Long userId,
        @Param("administrator") boolean administrator,
        @Param("search") String search,
        @Param("searchTitle") boolean searchTitle,
        @Param("searchOwner") boolean searchOwner,
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        Pageable pageable
    );

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

    @Query(
        value = """
            select survey
            from Survey survey
            join fetch survey.owner owner
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(owner.name) like concat('%', :search, '%')
            )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
            """,
        countQuery = """
            select count(survey.id)
            from Survey survey
            join survey.owner owner
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(owner.name) like concat('%', :search, '%')
            )
                and (:lifecycleStatus is null or survey.lifecycleStatus = :lifecycleStatus)
            """
    )
    Page<Survey> findDirectory(
        @Param("search") String search,
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        Pageable pageable
    );
}
