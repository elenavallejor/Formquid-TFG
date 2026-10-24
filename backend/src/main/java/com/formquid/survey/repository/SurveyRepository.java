package com.formquid.survey.repository;

import com.formquid.common.Category;
import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.dto.SurveyResultsSummaryDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accede a las encuestas y concentra las consultas optimizadas de listados y administración.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Modifying
    @Query("delete from Survey survey where survey.id = :surveyId")
    void deleteSurveyById(@Param("surveyId") Long surveyId);

    long countByOwnerId(Long ownerId);

    Optional<Survey> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);

    @Query(
        value = """
            select survey.*
            from surveys survey
            where survey.lifecycle_status = 'OPEN'
                and survey.visibility = 'LISTED'
                and (:includeAuthenticated = true or survey.access_mode <> 'AUTHENTICATED')
                and (:accessMode = '' or survey.access_mode = :accessMode)
                and (:categories = '' or survey.category = any(string_to_array(:categories, ',')))
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                    or exists (
                        select 1 from unnest(survey.tags) as searched_tag(tag_text)
                        where strpos(lower(searched_tag.tag_text), replace(:search, ' ', '-')) > 0
                    )
                )
                and (:tag = '' or exists (
                    select 1
                    from unnest(survey.tags) as survey_tag(tag_text)
                    where strpos(lower(survey_tag.tag_text), :tag) > 0
                ))
            order by survey.created_at desc, survey.id desc
            """,
        countQuery = """
            select count(survey.id)
            from surveys survey
            where survey.lifecycle_status = 'OPEN'
                and survey.visibility = 'LISTED'
                and (:includeAuthenticated = true or survey.access_mode <> 'AUTHENTICATED')
                and (:accessMode = '' or survey.access_mode = :accessMode)
                and (:categories = '' or survey.category = any(string_to_array(:categories, ',')))
                and (
                    :search = ''
                    or lower(survey.title) like concat('%', :search, '%')
                    or lower(coalesce(survey.description, '')) like concat('%', :search, '%')
                    or exists (
                        select 1 from unnest(survey.tags) as searched_tag(tag_text)
                        where strpos(lower(searched_tag.tag_text), replace(:search, ' ', '-')) > 0
                    )
                )
                and (:tag = '' or exists (
                    select 1
                    from unnest(survey.tags) as survey_tag(tag_text)
                    where strpos(lower(survey_tag.tag_text), :tag) > 0
                ))
            """,
        nativeQuery = true
    )
    Page<Survey> findListedOpenPage(
        @Param("includeAuthenticated") boolean includeAuthenticated,
        @Param("accessMode") String accessMode,
        @Param("categories") String categories,
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
                survey.updatedAt,
                survey.category
            )
            from Survey survey
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(survey.owner.name) like concat('%', :search, '%')
            )
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
                and (:allVisibilities = true or survey.visibility in :visibilities)
            """,
        countQuery = """
            select count(survey.id)
            from Survey survey
            where (
                :search = ''
                or lower(survey.title) like concat('%', :search, '%')
                or lower(survey.owner.name) like concat('%', :search, '%')
            )
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
                and (:allVisibilities = true or survey.visibility in :visibilities)
            """
    )
    Page<AdminSurveySummaryDto> findAdminSummaries(
        @Param("search") String search,
        @Param("allLifecycleStatuses") boolean allLifecycleStatuses,
        @Param("lifecycleStatuses") List<SurveyLifecycle> lifecycleStatuses,
        @Param("allVisibilities") boolean allVisibilities,
        @Param("visibilities") List<SurveyVisibility> visibilities,
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
                survey.updatedAt,
                survey.category
            )
            from Survey survey
            join Participation participation on participation.survey = survey
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
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
            group by
                survey.id,
                survey.title,
                survey.imageKey,
                owner.name,
                survey.lifecycleStatus,
                survey.updatedAt,
                survey.category
            """,
        countQuery = """
            select count(distinct survey.id)
            from Survey survey
            join Participation participation on participation.survey = survey
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
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
            """
    )
    Page<SurveyResultsSummaryDto> findResultSummaries(
        @Param("userId") Long userId,
        @Param("administrator") boolean administrator,
        @Param("search") String search,
        @Param("searchTitle") boolean searchTitle,
        @Param("searchOwner") boolean searchOwner,
        @Param("allLifecycleStatuses") boolean allLifecycleStatuses,
        @Param("lifecycleStatuses") List<SurveyLifecycle> lifecycleStatuses,
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
                and survey.accessMode in :accessModes
                and (:allCategories = true or survey.category in :categories)
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
                and survey.accessMode in :accessModes
                and (:allCategories = true or survey.category in :categories)
        """
    )
    Page<Survey> findEditableByUser(
        @Param("userId") Long userId,
        @Param("search") String search,
        @Param("lifecycleStatus") SurveyLifecycle lifecycleStatus,
        @Param("accessModes") List<SurveyAccessMode> accessModes,
        @Param("allCategories") boolean allCategories,
        @Param("categories") List<Category> categories,
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
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
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
                and (:allLifecycleStatuses = true or survey.lifecycleStatus in :lifecycleStatuses)
            """
    )
    Page<Survey> findDirectory(
        @Param("search") String search,
        @Param("allLifecycleStatuses") boolean allLifecycleStatuses,
        @Param("lifecycleStatuses") List<SurveyLifecycle> lifecycleStatuses,
        Pageable pageable
    );
}
