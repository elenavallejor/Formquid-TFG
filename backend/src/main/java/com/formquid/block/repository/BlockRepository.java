package com.formquid.block.repository;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, String> {

    List<Block> findByScopeOrderByNameAsc(BlockScope scope);

    Optional<Block> findByParentItemId(UUID parentItemId);
}
