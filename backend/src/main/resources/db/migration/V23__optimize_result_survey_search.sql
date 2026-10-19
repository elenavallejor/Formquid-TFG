-- Acelera la búsqueda de encuestas por título en la sección de resultados.

CREATE INDEX IF NOT EXISTS idx_surveys_title_search
    ON surveys USING gin (lower(title) gin_trgm_ops);
