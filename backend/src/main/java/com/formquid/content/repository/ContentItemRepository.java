package com.formquid.content.repository;

import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findBySurveyIdOrderByPositionAsc(String surveyId);

    List<ContentItem> findByTemplateIdOrderByPositionAsc(UUID templateId);

    List<ContentItem> findByParentBlockIdOrderByPositionAsc(String blockId);

    boolean existsBySurveyIdAndItemType(String surveyId, ContentItemType itemType);

    boolean existsByTemplateIdAndItemType(UUID templateId, ContentItemType itemType);

    boolean existsByParentBlockIdAndItemType(String blockId, ContentItemType itemType);

    @Modifying(flushAutomatically = true)
    @Query("update ContentItem item set item.position = :position where item.id = :itemId")
    void updatePosition(@Param("itemId") UUID itemId, @Param("position") Integer position);
}
