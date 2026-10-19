-- Añade un índice para consultar y ordenar usuarios por nombre.

CREATE INDEX IF NOT EXISTS idx_users_name
    ON users (name);