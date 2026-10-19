-- Añade un índice para paginar las encuestas públicas por fecha.

DROP INDEX IF EXISTS idx_surveys_lifecycle_visibility;

CREATE INDEX IF NOT EXISTS idx_surveys_public_listing
    ON surveys (lifecycle_status, visibility, created_at DESC, id DESC);
