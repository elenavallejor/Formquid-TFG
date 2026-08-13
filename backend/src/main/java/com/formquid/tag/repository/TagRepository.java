package com.formquid.tag.repository;

import com.formquid.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para etiquetas.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameNormalized(String nameNormalized);

    List<Tag> findByNameNormalizedIn(Collection<String> names);

    List<Tag> findAllByOrderByNameAsc();

    List<Tag> findByNameNormalizedContainingOrderByNameAsc(String query);
}