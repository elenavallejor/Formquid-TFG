package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, Long> {

    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);
    List<CollaborationRequest> findBySurveyIdOrderByCreatedAtDesc(Long surveyId);

    @Query("""
        select request
        from CollaborationRequest request
        join fetch request.survey survey
        join fetch survey.owner owner
        join fetch request.user requestedUser
        where owner.id = :userId or requestedUser.id = :userId
        order by request.createdAt desc
        """)
    List<CollaborationRequest> findRelatedToUser(@Param("userId") Long userId);

    @Query("""
        select request.survey.id
        from CollaborationRequest request
        where request.user.id = :userId
            and request.survey.id in :surveyIds
        """)
    List<Long> findSurveyIdsByUserIdAndSurveyIds(
        @Param("userId") Long userId,
        @Param("surveyIds") List<Long> surveyIds
    );
}
