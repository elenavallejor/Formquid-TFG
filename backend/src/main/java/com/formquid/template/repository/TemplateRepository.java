package com.formquid.template.repository;

import com.formquid.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findAllByOrderByTitleAsc();
}
