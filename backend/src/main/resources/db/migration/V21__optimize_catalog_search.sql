CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_templates_title_search
    ON templates USING gin (lower(title) gin_trgm_ops);

CREATE INDEX idx_templates_description_search
    ON templates USING gin (lower(description) gin_trgm_ops);

CREATE INDEX idx_blocks_name_search
    ON blocks USING gin (lower(name) gin_trgm_ops);

CREATE INDEX idx_blocks_description_search
    ON blocks USING gin (lower(description) gin_trgm_ops);

CREATE INDEX idx_questions_statement_search
    ON questions USING gin (lower(statement) gin_trgm_ops);

CREATE INDEX idx_questions_annotation_search
    ON questions USING gin (lower(annotation) gin_trgm_ops);

CREATE INDEX idx_users_name_search
    ON users USING gin (lower(name) gin_trgm_ops);
