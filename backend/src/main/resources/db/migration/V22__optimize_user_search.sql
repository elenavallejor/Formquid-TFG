-- Acelera las búsquedas de usuarios por correo e institución.

CREATE INDEX idx_users_email_search
    ON users USING gin (lower(email) gin_trgm_ops);

CREATE INDEX idx_users_institution_search
    ON users USING gin (lower(institution) gin_trgm_ops);
