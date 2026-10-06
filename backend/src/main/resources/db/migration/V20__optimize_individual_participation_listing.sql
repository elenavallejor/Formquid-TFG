DROP INDEX IF EXISTS idx_participations_survey_submitted;

CREATE INDEX idx_participations_survey_submitted
    ON participations (survey_id, submitted_at DESC, id DESC);
