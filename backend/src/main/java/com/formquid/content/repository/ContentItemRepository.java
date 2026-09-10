package com.formquid.content.repository;

import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {

    List<ContentItem> findBySurveyIdOrderByPositionAsc(Long surveyId);

    List<ContentItem> findByTemplateIdOrderByPositionAsc(Long templateId);

    List<ContentItem> findByParentBlockIdOrderByPositionAsc(Long blockId);

    boolean existsBySurveyIdAndItemType(Long surveyId, ContentItemType itemType);

    boolean existsByTemplateIdAndItemType(Long templateId, ContentItemType itemType);

    boolean existsByParentBlockIdAndItemType(Long blockId, ContentItemType itemType);

    @Modifying(flushAutomatically = true)
    @Query("update ContentItem item set item.position = :position where item.id = :itemId")
    void updatePosition(@Param("itemId") Long itemId, @Param("position") Integer position);
}