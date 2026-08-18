package com.formquid.participation.entity;

import com.formquid.question.entity.QuestionOption;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "answer_options")
public class AnswerOption {

    @EmbeddedId
    private AnswerOptionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("optionId")
    @JoinColumn(name = "option_id", nullable = false)
    private QuestionOption option;

    @Column(name = "other_text", columnDefinition = "text")
    private String otherText;
}
