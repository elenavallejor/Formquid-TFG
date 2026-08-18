package com.formquid.collaboration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class SurveyCollaboratorId implements Serializable {

    @Column(name = "survey_id", length = 20)
    private String surveyId;

    @Column(name = "user_id")
    private UUID userId;
}
