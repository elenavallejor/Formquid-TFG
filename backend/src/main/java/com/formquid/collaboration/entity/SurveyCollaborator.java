package com.formquid.collaboration.entity;

import com.formquid.survey.entity.Survey;
import com.formquid.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa la colaboración de un editor en una encuesta.
 *
 * Un editor colaborador puede acceder a la encuesta según los permisos
 * definidos en SurveyAccessService.
 */
@Entity
@Table(
    name = "survey_collaborators",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_survey_collaborators_survey_editor",
            columnNames = {"survey_id", "editor_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_survey_collaborators_survey", columnList = "survey_id"),
        @Index(name = "idx_survey_collaborators_editor", columnList = "editor_user_id"),
        @Index(name = "idx_survey_collaborators_added_by", columnList = "added_by_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Encuesta en la que participa el colaborador.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    /**
     * Editor que colabora en la encuesta.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private User editor;

    /**
     * Usuario que añadió al colaborador.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    /**
     * Fecha en la que se añadió la colaboración.
     */
    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}