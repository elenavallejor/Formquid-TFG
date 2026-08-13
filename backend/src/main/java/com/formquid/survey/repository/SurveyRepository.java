package com.formquid.survey.repository;

import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para encuestas.
 *
 * Incluye consultas privadas, administrativas y públicas, cargando
 * únicamente las relaciones necesarias para cada contexto de uso.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    /**
     * Recupera una encuesta interna cargando propietario y etiquetas.
     *
     * Los bloques y preguntas permanecen con carga diferida y se benefician
     * de @BatchSize en las entidades.
     */
    @Override
    @EntityGraph(attributePaths = {"owner", "tags"})
    Optional<Survey> findById(Long id);

    /**
     * Recupera una encuesta para acceso público.
     *
     * Se cargan etiquetas y bloques. Las preguntas de los bloques se
     * recuperan por lotes mediante @BatchSize en SurveyBlock.
     */
    @EntityGraph(attributePaths = {"tags", "blocks"})
    @Query("""
        select distinct survey
        from Survey survey
        where survey.publicCode = :publicCode
    """)
    Optional<Survey> findPublicStructureByPublicCode(
        @Param("publicCode") String publicCode
    );

    /**
     * Lista encuestas públicas para la página de inicio.
     *
     * Solo se precargan las etiquetas porque no se necesita la estructura
     * completa en este listado.
     */
    @EntityGraph(attributePaths = {"tags"})
    @Query("""
        select distinct survey
        from Survey survey
        where survey.status = :status
        order by survey.createdAt desc
    """)
    List<Survey> findPublicByStatusOrderByCreatedAtDesc(
        @Param("status") SurveyStatus status
    );

    /**
     * Recupera una encuesta durante el envío de respuestas.
     *
     * El bloqueo pesimista evita cambios incompatibles mientras se valida
     * y registra una participación.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        select survey
        from Survey survey
        where survey.publicCode = :publicCode
    """)
    Optional<Survey> findByPublicCodeForAnswer(
        @Param("publicCode") String publicCode
    );

    /**
     * Lista las encuestas creadas por un usuario.
     */
    @EntityGraph(attributePaths = {"owner", "tags"})
    @Query("""
        select distinct survey
        from Survey survey
        where survey.owner.id = :ownerId
        order by survey.createdAt desc
    """)
    List<Survey> findByOwnerIdOrderByCreatedAtDesc(
        @Param("ownerId") Long ownerId
    );

    /**
     * Lista encuestas filtradas por estado.
     */
    @EntityGraph(attributePaths = {"owner", "tags"})
    @Query("""
        select distinct survey
        from Survey survey
        where survey.status = :status
        order by survey.createdAt desc
    """)
    List<Survey> findByStatusOrderByCreatedAtDesc(
        @Param("status") SurveyStatus status
    );

    /**
     * Lista todas las encuestas para administración.
     */
    @EntityGraph(attributePaths = {"owner", "tags"})
    @Query("""
        select distinct survey
        from Survey survey
        order by survey.createdAt desc
    """)
    List<Survey> findAllByOrderByCreatedAtDesc();

    /**
     * Cuenta encuestas de un determinado estado.
     */
    long countByStatus(SurveyStatus status);
}
