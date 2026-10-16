package com.formquid.template.repository;

import com.formquid.common.Category;
import com.formquid.template.dto.TemplateSummaryDto;
import com.formquid.template.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Gestiona el acceso a plantillas y el listado ligero del catálogo.
 */
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query(
        value = """
            select new com.formquid.template.dto.TemplateSummaryDto(
                template.id,
                template.title,
                template.description,
                template.imageKey,
                creator.name,
                coalesce(sum(
                    case
                        when item.itemType = com.formquid.content.entity.ContentItemType.QUESTION then 1
                        when item.itemType = com.formquid.content.entity.ContentItemType.BLOCK
                            and blockItem.itemType = com.formquid.content.entity.ContentItemType.QUESTION then 1
                        else 0
                    end
                ), 0),
                template.createdAt,
                template.updatedAt,
                template.category
            )
            from Template template
            left join template.createdBy creator
            left join template.contentItems item
            left join item.block nestedBlock
            left join nestedBlock.contentItems blockItem
            where
                (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
                and (:category is null or template.category = :category)
                and (
                    :search = ''
                    or (:searchTitle = true and lower(template.title) like concat('%', :search, '%'))
                    or (:searchDescription = true and lower(coalesce(template.description, '')) like concat('%', :search, '%'))
                    or (:searchCreator = true and lower(coalesce(creator.name, '')) like concat('%', :search, '%'))
                )
            group by
                template.id,
                template.title,
                template.description,
                template.imageKey,
                creator.name,
                template.createdAt,
                template.updatedAt,
                template.category
            """,
        countQuery = """
            select count(template.id)
            from Template template
            left join template.createdBy creator
            where
                (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
                and (:category is null or template.category = :category)
                and (
                    :search = ''
                    or (:searchTitle = true and lower(template.title) like concat('%', :search, '%'))
                    or (:searchDescription = true and lower(coalesce(template.description, '')) like concat('%', :search, '%'))
                    or (:searchCreator = true and lower(coalesce(creator.name, '')) like concat('%', :search, '%'))
                )
            """
    )
    Page<TemplateSummaryDto> findCatalogSummaries(
        @Param("userId") Long userId,
        @Param("mine") boolean mine,
        @Param("search") String search,
        @Param("searchTitle") boolean searchTitle,
        @Param("searchDescription") boolean searchDescription,
        @Param("searchCreator") boolean searchCreator,
        @Param("category") Category category,
        Pageable pageable
    );
}
