-- scripts/migrate-auth-users-to-region.sql
-- One-shot migration: copy users from gcrf_auth.users → gcrf_region.users via dblink.
--
-- Prerequisites:
--   1. Connected to gcrf_main as superuser (psql -U postgres -d gcrf_main)
--   2. V001__iam_baseline.sql has already run (gcrf_region.users exists and admin is seeded)
--   3. gcrf_auth database still exists with users table intact
--
-- Usage (via run-auth-user-migration.sh):
--   POSTGRES_PASSWORD=<pass> bash scripts/run-auth-user-migration.sh \
--       -U postgres -d gcrf_main -h <host>
--
-- The placeholder __OLD_DB_PASS__ is substituted by run-auth-user-migration.sh before execution.

CREATE EXTENSION IF NOT EXISTS dblink;

DO $$
DECLARE
    v_old_conn TEXT := 'host=postgresql.edu-infra.svc.cluster.local port=5432 dbname=gcrf_auth user=postgres password=__OLD_DB_PASS__';
    rows_before INT;
    rows_after  INT;
BEGIN
    SELECT count(*) INTO rows_before FROM gcrf_region.users;

    INSERT INTO gcrf_region.users (
        user_id, username, password, email, phone, user_type,
        avatar_url, status, last_login_time, last_login_ip,
        failed_login_count, locked_until, created_at, updated_at, deleted_at
    )
    SELECT
        user_id,
        username,
        password,
        email,
        phone,
        COALESCE(user_type, 'STUDENT'),
        avatar_url,
        COALESCE(status, 'ACTIVE'),
        last_login_time::TIMESTAMPTZ,
        last_login_ip,
        COALESCE(failed_login_count, 0),
        locked_until::TIMESTAMPTZ,
        COALESCE(created_at, NOW())::TIMESTAMPTZ,
        COALESCE(updated_at, NOW())::TIMESTAMPTZ,
        deleted_at::TIMESTAMPTZ
    FROM dblink(
        v_old_conn,
        'SELECT user_id, username, password, email, phone, user_type,
                avatar_url, status, last_login_time, last_login_ip,
                failed_login_count, locked_until, created_at, updated_at, deleted_at
         FROM users
         WHERE deleted_at IS NULL'
    ) AS old(
        user_id            VARCHAR,
        username           VARCHAR,
        password           VARCHAR,
        email              VARCHAR,
        phone              VARCHAR,
        user_type          VARCHAR,
        avatar_url         VARCHAR,
        status             VARCHAR,
        last_login_time    TIMESTAMP,
        last_login_ip      VARCHAR,
        failed_login_count INT,
        locked_until       TIMESTAMP,
        created_at         TIMESTAMP,
        updated_at         TIMESTAMP,
        deleted_at         TIMESTAMP
    )
    ON CONFLICT (user_id) DO NOTHING;

    SELECT count(*) INTO rows_after FROM gcrf_region.users;

    RAISE NOTICE 'Migration complete: rows_before=%, rows_after=%, inserted=%',
        rows_before, rows_after, (rows_after - rows_before);
END $$;

-- Reset sequence to avoid PK collisions on next INSERT
SELECT setval(
    pg_get_serial_sequence('gcrf_region.users', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 0) FROM gcrf_region.users) + 1, 1),
    false
);

-- Sanity check: compare counts between old and new
SELECT
    (SELECT count(*) FROM dblink(
        'host=postgresql.edu-infra.svc.cluster.local port=5432 dbname=gcrf_auth user=postgres password=__OLD_DB_PASS__',
        'SELECT 1 FROM users WHERE deleted_at IS NULL'
    ) AS t(x INT)) AS old_active_count,
    (SELECT count(*) FROM gcrf_region.users WHERE deleted_at IS NULL) AS new_active_count;
