-- Simplifica la protección que impide quedarse sin superadministradores.

DROP TRIGGER IF EXISTS trg_users_reassign_superadmin_guard ON users;
DROP FUNCTION IF EXISTS reassign_superadmin_guard();
DROP TABLE IF EXISTS superadmin_guard;
DROP FUNCTION IF EXISTS prevent_superadmin_guard_removal();

ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_role_id;

CREATE OR REPLACE FUNCTION prevent_no_superadmin()
RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE role = 'SUPERADMIN') THEN
        RAISE EXCEPTION 'Debe existir siempre un superadministrador';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_keep_superadmin_after_delete
AFTER DELETE ON users
FOR EACH STATEMENT EXECUTE FUNCTION prevent_no_superadmin();

CREATE TRIGGER trg_users_keep_superadmin_after_update
AFTER UPDATE OF role ON users
FOR EACH STATEMENT EXECUTE FUNCTION prevent_no_superadmin();
