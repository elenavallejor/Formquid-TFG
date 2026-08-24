-- Índices para los listados y contadores utilizados por administración.

CREATE INDEX IF NOT EXISTS idx_registration_requests_created_at
    ON registration_requests (created_at);

CREATE INDEX IF NOT EXISTS idx_surveys_updated_at
    ON surveys (updated_at);

-- Índice para el contador de notificaciones no leídas.
CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications (recipient_user_id, channel)
    WHERE read_at IS NULL;

-- El UNIQUE de users.email ya crea su propio índice en PostgreSQL.
DROP INDEX IF EXISTS idx_users_email;
