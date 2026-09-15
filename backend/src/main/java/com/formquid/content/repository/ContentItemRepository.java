package com.formquid.content.repository;

import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {

    List<ContentItem> findBySurveyIdOrderByPositionAsc(Long surveyId);

    List<ContentItem> findByTemplateIdOrderByPositionAsc(Long templateId);

    List<ContentItem> findByParentBlockIdOrderByPositionAsc(Long blockId);

    @Query("""
        select new com.formquid.content.dto.BlockContentItemRowDto(
            item.parentBlock.id,
            item.id,
            item.itemType,
            item.position,
            item.required,
            item.textBody
        )
        from ContentItem item
        where item.parentBlock.id in :blockIds
        order by item.parentBlock.id, item.position
        """)
    List<BlockContentItemRowDto> findBlockContentRows(
        @Param("blockIds") Collection<Long> blockIds
    );

    boolean existsBySurveyIdAndItemType(Long surveyId, ContentItemType itemType);

    boolean existsByTemplateIdAndItemType(Long templateId, ContentItemType itemType);

    boolean existsByParentBlockIdAndItemType(Long blockId, ContentItemType itemType);

}
