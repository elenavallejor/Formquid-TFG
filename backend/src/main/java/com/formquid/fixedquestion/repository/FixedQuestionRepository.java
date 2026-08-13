package com.formquid.fixedquestion.repository;

import com.formquid.fixedquestion.entity.FixedQuestion;
import com.formquid.fixedquestion.entity.FixedQuestionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repositorio de acceso a datos para preguntas fijas.
 */
public interface FixedQuestionRepository
    extends JpaRepository<FixedQuestion, Long> {

    @EntityGraph(attributePaths = {"block"})
    List<FixedQuestion> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"block"})
    List<FixedQuestion> findByStatusOrderByTextAsc(
        FixedQuestionStatus status
    );

    @EntityGraph(attributePaths = {"block"})
    List<FixedQuestion> findByBlockIdAndStatusOrderByTextAsc(
        Long blockId,
        FixedQuestionStatus status
    );

    boolean existsByBlockIdAndStatus(
        Long blockId,
        FixedQuestionStatus status
    );

    /**
     * Obtiene varias preguntas fijas en una sola consulta.
     *
     * Se carga también el bloque asociado para evitar consultas adicionales
     * al validar que la pregunta pertenece al bloque correcto.
     */
    @EntityGraph(attributePaths = {"block"})
    List<FixedQuestion> findByIdIn(Collection<Long> ids);

    /**
     * Obtiene los bloques que contienen al menos una pregunta fija activa.
     *
     * Se usa para validar en lote que un bloque es utilizable, evitando
     * llamar a existsByBlockIdAndStatus por cada bloque.
     */
    @Query("""
        select distinct question.block.id
        from FixedQuestion question
        where question.block.id in :blockIds
          and question.status = :status
    """)
    Set<Long> findBlockIdsWithActiveQuestions(
        @Param("blockIds") Collection<Long> blockIds,
        @Param("status") FixedQuestionStatus status
    );
}