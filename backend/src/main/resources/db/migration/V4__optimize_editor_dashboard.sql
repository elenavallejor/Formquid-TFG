-- Añade índices para cargar más rápido las encuestas propias y compartidas del panel.

CREATE INDEX IF NOT EXISTS idx_surveys_owner_updated_at
    ON surveys (owner_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_survey_collaborators_user_survey
    ON survey_collaborators (user_id, survey_id);