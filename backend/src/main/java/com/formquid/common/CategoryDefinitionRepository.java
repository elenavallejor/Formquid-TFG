package com.formquid.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryDefinitionRepository extends JpaRepository<CategoryDefinition, String> {
    List<CategoryDefinition> findAllByOrderByNameAsc();
}
