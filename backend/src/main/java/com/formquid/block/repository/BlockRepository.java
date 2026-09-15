package com.formquid.block.repository;

import com.formquid.block.dto.BlockSummaryDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    List<Block> findByScopeOrderByNameAsc(BlockScope scope);

    @Query(
        value = """
            select new com.formquid.block.dto.BlockSummaryDto(
                block.id,
                block.name,
                block.description,
                creator.name,
                count(item.id),
                block.createdAt,
                block.updatedAt
            )
            from Block block
            left join block.createdBy creator
            left join block.contentItems item
            where block.scope = :scope
                and (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
            group by
                block.id,
                block.name,
                block.description,
                creator.name,
                block.createdAt,
                block.updatedAt
            """,
        countQuery = """
            select count(block.id)
            from Block block
            left join block.createdBy creator
            where block.scope = :scope
                and (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
            """
    )
    Page<BlockSummaryDto> findCatalogSummaries(
        @Param("scope") BlockScope scope,
        @Param("userId") Long userId,
        @Param("mine") boolean mine,
        Pageable pageable
    );

    @Query(
        value = """
            select block
            from Block block
            where block.scope = :scope
            """,
        countQuery = """
            select count(block.id)
            from Block block
            where block.scope = :scope
            """
    )
    Page<Block> findCatalogBlocksWithDetails(
        @Param("scope") BlockScope scope,
        Pageable pageable
    );

    Optional<Block> findByParentItemId(Long parentItemId);

    long countBySourceBlockId(Long sourceBlockId);
}
