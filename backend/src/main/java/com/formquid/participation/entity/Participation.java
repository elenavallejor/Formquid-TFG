package com.formquid.participation.entity;

import com.formquid.survey.entity.Survey;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una participación anónima en una encuesta.
 *
 * Cada participación agrupa todas las respuestas enviadas por una persona.
 * No se almacena información identificativa del encuestado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "participations",
    indexes = {
        @Index(name = "idx_participations_survey_id", columnList = "survey_id"),
        @Index(name = "idx_participations_survey_submitted_at", columnList = "survey_id, submitted_at")
    }
)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Encuesta respondida.
     *
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    /**
     * Respuestas incluidas en la participación.
     */
    @OneToMany(
        mappedBy = "participation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 50)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    /**
     * Fecha de envío de la participación.
     */
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /**
     * Añade una respuesta manteniendo la relación bidireccional.
     */
    public void addAnswer(Answer answer) {
        answers.add(answer);
        answer.setParticipation(this);
    }

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}