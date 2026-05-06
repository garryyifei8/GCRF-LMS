-- Enable trigram extension for full-text search on text fields
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Cross-school book search materialized view, lazily populated.
-- Initial create is empty; refresh_book_search_mview() rebuilds it from
-- all school_* schemas detected at runtime.
CREATE MATERIALIZED VIEW IF NOT EXISTS book_search_mview AS
SELECT
    NULL::TEXT       AS school_schema,
    NULL::BIGINT     AS book_id,
    NULL::TEXT       AS isbn,
    NULL::TEXT       AS title,
    NULL::TEXT       AS author,
    NULL::TEXT       AS classification,
    NULL::INT        AS total_count,
    NULL::INT        AS available_count,
    NULL::TIMESTAMPTZ AS created_at
WHERE FALSE;

-- Trigram indexes for full-text search efficiency
CREATE INDEX IF NOT EXISTS idx_book_search_title_trgm
    ON book_search_mview USING GIN (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_book_search_author_trgm
    ON book_search_mview USING GIN (author gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_book_search_isbn_trgm
    ON book_search_mview USING GIN (isbn gin_trgm_ops);

-- Standard indexes for filtering
CREATE INDEX IF NOT EXISTS idx_book_search_clc
    ON book_search_mview (classification);
CREATE INDEX IF NOT EXISTS idx_book_search_school
    ON book_search_mview (school_schema);

-- Refresh function: rebuilds the mview from all school_* schemas.
-- Discovers school schemas via pg_namespace, then UNION ALL all school_*.book_catalog tables.
-- Drops and recreates the materialized view (CONCURRENTLY refresh requires a unique index,
-- which will be added in a later migration for plan-C2).
CREATE OR REPLACE FUNCTION refresh_book_search_mview() RETURNS INT AS $$
DECLARE
    sch       TEXT;
    union_sql TEXT := '';
    row_count INT;
BEGIN
    -- Ensure dynamic SQL (EXECUTE) can resolve pg_trgm operators in public schema
    SET LOCAL search_path = gcrf_region, public;

    -- Iterate over all school_* schemas, ordered by name
    FOR sch IN
        SELECT nspname FROM pg_namespace
        WHERE nspname LIKE 'school\_%' ESCAPE '\'
        ORDER BY nspname
    LOOP
        -- Append UNION ALL between queries
        IF union_sql <> '' THEN
            union_sql := union_sql || ' UNION ALL ';
        END IF;
        -- Dynamically build SELECT from each school_*.book_catalog table
        union_sql := union_sql || format($f$
            SELECT %L AS school_schema, id AS book_id, isbn, title, author,
                   classification, total_count, available_count, created_at
            FROM %I.book_catalog
        $f$, sch, sch);
    END LOOP;

    -- Drop and recreate; CONCURRENTLY refresh requires a unique index, leave for plan-C2
    EXECUTE 'DROP MATERIALIZED VIEW IF EXISTS book_search_mview CASCADE';

    IF union_sql = '' THEN
        -- No school schemas found: recreate empty view
        EXECUTE $sql$
            CREATE MATERIALIZED VIEW book_search_mview AS
            SELECT NULL::TEXT  AS school_schema,
                   NULL::BIGINT AS book_id,
                   NULL::TEXT  AS isbn,
                   NULL::TEXT  AS title,
                   NULL::TEXT  AS author,
                   NULL::TEXT  AS classification,
                   NULL::INT   AS total_count,
                   NULL::INT   AS available_count,
                   NULL::TIMESTAMPTZ AS created_at
            WHERE FALSE
        $sql$;
    ELSE
        -- Create materialized view from UNION ALL of all school schemas
        EXECUTE 'CREATE MATERIALIZED VIEW book_search_mview AS ' || union_sql;
    END IF;

    -- Recreate all indexes after DROP CASCADE
    EXECUTE 'CREATE INDEX idx_book_search_title_trgm  ON book_search_mview USING GIN (title gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_author_trgm ON book_search_mview USING GIN (author gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_isbn_trgm   ON book_search_mview USING GIN (isbn gin_trgm_ops)';
    EXECUTE 'CREATE INDEX idx_book_search_clc         ON book_search_mview (classification)';
    EXECUTE 'CREATE INDEX idx_book_search_school      ON book_search_mview (school_schema)';

    -- Return row count of refreshed materialized view
    SELECT count(*) INTO row_count FROM book_search_mview;
    RETURN row_count;
END;
$$ LANGUAGE plpgsql;
