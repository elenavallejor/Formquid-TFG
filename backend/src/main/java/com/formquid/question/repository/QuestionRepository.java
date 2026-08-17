package com.formquid.question.repository;

import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repositorio de acceso a datos para preguntas.
 */
public interface QuestionRepository
    extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = {"block"})
    List<Question> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"block"})
    List<Question> findByStatusOrderByTextAsc(
        QuestionStatus status
    );

    @EntityGraph(attributePaths = {"block"})
    List<Question> findByBlockIdAndStatusOrderByTextAsc(
        Long blockId,
        QuestionStatus status
    );

    boolean existsByBlockIdAndStatus(
        Long blockId,
        QuestionStatus status
    );

    /**
     * Obtiene varias preguntas en una sola consulta.
     *
     * Se carga también el bloque asociado para evitar consultas adicionales
     * al validar que la pregunta pertenece al bloque correcto.
     */
    @EntityGraph(attributePaths = {"block"})
    List<Question> findByIdIn(Collection<Long> ids);

    /**
     * Obtiene los bloques que contienen al menos una pregunta activa.
     *
     * Se usa para validar en lote que un bloque es utilizable, evitando
     * llamar a existsByBlockIdAndStatus por cada bloque.
     */
    @Query("""
        select distinct question.block.id
        from Question question
        where question.block.id in :blockIds
          and question.status = :status
    """)
    Set<Long> findBlockIdsWithActiveQuestions(
        @Param("blockIds") Collection<Long> blockIds,
        @Param("status") QuestionStatus status
    );
}
