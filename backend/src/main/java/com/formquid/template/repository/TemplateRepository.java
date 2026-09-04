package com.formquid.template.repository;

import com.formquid.template.dto.TemplateSummaryDto;
import com.formquid.template.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona el acceso a plantillas y el listado ligero del catálogo.
 */
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findAllByOrderByTitleAsc();

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
                template.updatedAt
            )
            from Template template
            left join template.createdBy creator
            left join template.contentItems item
            left join item.block nestedBlock
            left join nestedBlock.contentItems blockItem
            group by
                template.id,
                template.title,
                template.description,
                template.imageKey,
                creator.name,
                template.updatedAt
            """,
        countQuery = """
            select count(template.id)
            from Template template
            """
    )
    Page<TemplateSummaryDto> findCatalogSummaries(Pageable pageable);
}