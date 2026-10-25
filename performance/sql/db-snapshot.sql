-- Ejecutar con psql contra la misma base de pruebas durante cada fase.
-- Los contadores de pg_stat_database son acumulados; comparar capturas con hora.
SELECT clock_timestamp() AS captured_at, current_database() AS database_name;

SELECT 'surveys' AS table_name, count(*) AS rows FROM surveys
UNION ALL
SELECT 'content_items', count(*) FROM content_items
UNION ALL
SELECT 'participations', count(*) FROM participations;

SELECT clock_timestamp() AS captured_at, datname, numbackends,
       xact_commit, xact_rollback, blks_read, blks_hit,
       tup_returned, tup_fetched, tup_inserted, tup_updated, tup_deleted,
       deadlocks, temp_files, temp_bytes
FROM pg_stat_database
WHERE datname = current_database();

SELECT clock_timestamp() AS captured_at, state, wait_event_type, wait_event,
       count(*) AS connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state, wait_event_type, wait_event
ORDER BY connections DESC, state;

SELECT clock_timestamp() AS captured_at, pid, state, wait_event_type, wait_event,
       clock_timestamp() - query_start AS query_age,
       left(query, 200) AS query_sample
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid()
  AND wait_event_type = 'Lock'
ORDER BY query_start;
