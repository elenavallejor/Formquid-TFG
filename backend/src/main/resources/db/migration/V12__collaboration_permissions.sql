-- Distingue los permisos de consulta y edición en las colaboraciones y sus solicitudes.

ALTER TABLE survey_collaborators
    ADD COLUMN IF NOT EXISTS permission varchar(20) NOT NULL DEFAULT 'EDIT';

ALTER TABLE collaboration_requests
    ADD COLUMN IF NOT EXISTS permission varchar(20) NOT NULL DEFAULT 'EDIT';

ALTER TABLE survey_collaborators
    DROP CONSTRAINT IF EXISTS ck_survey_collaborators_permission;

ALTER TABLE survey_collaborators
    ADD CONSTRAINT ck_survey_collaborators_permission
    CHECK (permission IN ('VIEW', 'EDIT'));

ALTER TABLE collaboration_requests
    DROP CONSTRAINT IF EXISTS ck_collaboration_requests_permission;

ALTER TABLE collaboration_requests
    ADD CONSTRAINT ck_collaboration_requests_permission
    CHECK (permission IN ('VIEW', 'EDIT'));
