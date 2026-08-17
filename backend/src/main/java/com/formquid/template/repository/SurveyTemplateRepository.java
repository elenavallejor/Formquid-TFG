package com.formquid.template.repository;

import com.formquid.template.entity.SurveyTemplate;
import com.formquid.template.entity.TemplateStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para plantillas reutilizables.
 */
public interface SurveyTemplateRepository
    extends JpaRepository<SurveyTemplate, Long> {

    /**
     * Busca una plantilla por su nombre normalizado.
     */
    Optional<SurveyTemplate> findByNameNormalized(
        String nameNormalized
    );

    /**
     * Comprueba si ya existe una plantilla con el nombre normalizado indicado.
     */
    boolean existsByNameNormalized(String nameNormalized);

    /**
     * Obtiene todas las plantillas ordenadas por fecha de creación descendente.
     */
    @EntityGraph(
        attributePaths = {
            "createdBy"
        }
    )
    List<SurveyTemplate> findAllByOrderByCreatedAtDesc();

    /**
     * Obtiene plantillas por estado ordenadas alfabéticamente.
     */
    @EntityGraph(
        attributePaths = {
            "createdBy"
        }
    )
    List<SurveyTemplate> findByStatusOrderByNameAsc(
        TemplateStatus status
    );

    /**
     * Obtiene una plantilla con su creador cargado.
     */
    @EntityGraph(
        attributePaths = {
            "createdBy"
        }
    )
    @Query("""
        select template
        from SurveyTemplate template
        where template.id = :id
    """)
    Optional<SurveyTemplate> findDetailedById(
        @Param("id") Long id
    );
}
