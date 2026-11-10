CREATE TABLE survey_cover_files (
    filename varchar(40) PRIMARY KEY,
    survey_id bigint NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    content_type varchar(10) NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg')),
    image_bytes bytea NOT NULL
);

CREATE INDEX idx_survey_cover_files_survey_id ON survey_cover_files (survey_id);
