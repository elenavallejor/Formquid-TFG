ALTER TABLE users ADD COLUMN country varchar(100);
ALTER TABLE registration_requests ADD COLUMN country varchar(100);

CREATE INDEX idx_users_country_search
    ON users USING gin (lower(country) gin_trgm_ops);
