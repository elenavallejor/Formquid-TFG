-- Evita registrar participaciones duplicadas con el mismo usuario o identificador de participante.

ALTER TABLE participations
    ADD COLUMN respondent_user_id bigint,
    ADD COLUMN respondent_token_hash varchar(64),
    ADD CONSTRAINT fk_participations_respondent_user
        FOREIGN KEY (respondent_user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_participations_single_respondent
        CHECK (respondent_user_id IS NULL OR respondent_token_hash IS NULL);

CREATE UNIQUE INDEX ux_participations_survey_respondent_user
    ON participations(survey_id, respondent_user_id)
    WHERE respondent_user_id IS NOT NULL;

CREATE UNIQUE INDEX ux_participations_survey_respondent_token
    ON participations(survey_id, respondent_token_hash)
    WHERE respondent_token_hash IS NOT NULL;
