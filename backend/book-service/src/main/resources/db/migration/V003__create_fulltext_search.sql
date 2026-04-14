-- =========================================
-- V003: Full-Text Search Implementation
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "unaccent";  -- Remove accents from text
CREATE EXTENSION IF NOT EXISTS "pg_trgm";   -- Trigram similarity search

-- Note: For Chinese support, you may need zhparser extension
-- If available: CREATE EXTENSION IF NOT EXISTS "zhparser";

-- =========================================
-- 1. Add Full-Text Search Vector Column
-- =========================================

-- Add tsvector column for full-text search
ALTER TABLE books ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Add weighted search document column (for ranking)
ALTER TABLE books ADD COLUMN IF NOT EXISTS search_document TEXT GENERATED ALWAYS AS (
    COALESCE(title, '') || ' ' ||
    COALESCE(subtitle, '') || ' ' ||
    COALESCE(author, '') || ' ' ||
    COALESCE(translator, '') || ' ' ||
    COALESCE(publisher, '') || ' ' ||
    COALESCE(isbn, '') || ' ' ||
    COALESCE(subject_keywords, '') || ' ' ||
    COALESCE(abstract, '') || ' ' ||
    COALESCE(classification_code, '')
) STORED;

-- =========================================
-- 2. Create Custom Text Search Configuration
-- =========================================

-- Create custom text search configuration for mixed language support
DROP TEXT SEARCH CONFIGURATION IF EXISTS book_search CASCADE;
CREATE TEXT SEARCH CONFIGURATION book_search (COPY = pg_catalog.simple);

-- If zhparser is available, uncomment the following:
-- CREATE TEXT SEARCH CONFIGURATION book_search (PARSER = zhparser);
-- ALTER TEXT SEARCH CONFIGURATION book_search ADD MAPPING FOR n,v,a,i,e,l,j WITH simple;

-- =========================================
-- 3. Create GIN Indexes for Performance
-- =========================================

-- Main full-text search index
CREATE INDEX IF NOT EXISTS idx_books_search_vector
    ON books USING gin(search_vector)
    WHERE deleted_at IS NULL;

-- Trigram indexes for fuzzy search
CREATE INDEX IF NOT EXISTS idx_books_title_trgm
    ON books USING gin(title gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_books_author_trgm
    ON books USING gin(author gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_books_isbn_trgm
    ON books USING gin(isbn gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Combined search document index
CREATE INDEX IF NOT EXISTS idx_books_search_document
    ON books USING gin(to_tsvector('simple', search_document))
    WHERE deleted_at IS NULL;

-- =========================================
-- 4. Functions for Full-Text Search
-- =========================================

-- Function to update search vector with weighted terms
CREATE OR REPLACE FUNCTION update_book_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.subtitle, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.author, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.translator, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.publisher, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.isbn, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.subject_keywords, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.abstract, '')), 'D') ||
        setweight(to_tsvector('simple', COALESCE(NEW.classification_code, '')), 'C');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Advanced search function with multiple strategies
CREATE OR REPLACE FUNCTION search_books(
    p_query TEXT,
    p_search_type VARCHAR DEFAULT 'smart',  -- 'exact', 'fuzzy', 'smart'
    p_limit INTEGER DEFAULT 50,
    p_offset INTEGER DEFAULT 0
) RETURNS TABLE (
    id BIGINT,
    isbn VARCHAR,
    title VARCHAR,
    subtitle VARCHAR,
    author VARCHAR,
    publisher VARCHAR,
    publish_date DATE,
    available_copies INTEGER,
    score REAL,
    match_type VARCHAR
) AS $$
BEGIN
    -- Normalize query
    p_query := TRIM(p_query);

    IF p_query = '' OR p_query IS NULL THEN
        RETURN;
    END IF;

    -- Smart search: Try exact first, then fuzzy
    IF p_search_type = 'smart' THEN
        -- First try exact full-text search
        RETURN QUERY
        SELECT
            b.id,
            b.isbn,
            b.title,
            b.subtitle,
            b.author,
            b.publisher,
            b.publish_date,
            b.available_copies,
            ts_rank(b.search_vector, plainto_tsquery('simple', p_query)) AS score,
            'fulltext'::VARCHAR AS match_type
        FROM books b
        WHERE b.deleted_at IS NULL
            AND b.status = 'ACTIVE'
            AND b.search_vector @@ plainto_tsquery('simple', p_query)
        ORDER BY score DESC, b.title
        LIMIT p_limit
        OFFSET p_offset;

        -- If no results, try fuzzy search
        IF NOT FOUND THEN
            RETURN QUERY
            SELECT * FROM search_books_fuzzy(p_query, p_limit, p_offset);
        END IF;

    -- Exact full-text search
    ELSIF p_search_type = 'exact' THEN
        RETURN QUERY
        SELECT
            b.id,
            b.isbn,
            b.title,
            b.subtitle,
            b.author,
            b.publisher,
            b.publish_date,
            b.available_copies,
            ts_rank(b.search_vector, plainto_tsquery('simple', p_query)) AS score,
            'exact'::VARCHAR AS match_type
        FROM books b
        WHERE b.deleted_at IS NULL
            AND b.status = 'ACTIVE'
            AND b.search_vector @@ plainto_tsquery('simple', p_query)
        ORDER BY score DESC, b.title
        LIMIT p_limit
        OFFSET p_offset;

    -- Fuzzy search using trigrams
    ELSIF p_search_type = 'fuzzy' THEN
        RETURN QUERY
        SELECT * FROM search_books_fuzzy(p_query, p_limit, p_offset);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Fuzzy search function using trigrams
CREATE OR REPLACE FUNCTION search_books_fuzzy(
    p_query TEXT,
    p_limit INTEGER DEFAULT 50,
    p_offset INTEGER DEFAULT 0
) RETURNS TABLE (
    id BIGINT,
    isbn VARCHAR,
    title VARCHAR,
    subtitle VARCHAR,
    author VARCHAR,
    publisher VARCHAR,
    publish_date DATE,
    available_copies INTEGER,
    score REAL,
    match_type VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        b.id,
        b.isbn,
        b.title,
        b.subtitle,
        b.author,
        b.publisher,
        b.publish_date,
        b.available_copies,
        GREATEST(
            similarity(b.title, p_query),
            similarity(b.author, p_query),
            similarity(b.isbn, p_query) * 0.8,
            similarity(COALESCE(b.subject_keywords, ''), p_query) * 0.6
        ) AS score,
        'fuzzy'::VARCHAR AS match_type
    FROM books b
    WHERE b.deleted_at IS NULL
        AND b.status = 'ACTIVE'
        AND (
            b.title % p_query OR
            b.author % p_query OR
            b.isbn % p_query OR
            COALESCE(b.subject_keywords, '') % p_query
        )
    ORDER BY score DESC, b.title
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Advanced search with filters
CREATE OR REPLACE FUNCTION search_books_advanced(
    p_query TEXT,
    p_category_id BIGINT DEFAULT NULL,
    p_publisher VARCHAR DEFAULT NULL,
    p_publish_year_from INTEGER DEFAULT NULL,
    p_publish_year_to INTEGER DEFAULT NULL,
    p_price_min DECIMAL DEFAULT NULL,
    p_price_max DECIMAL DEFAULT NULL,
    p_available_only BOOLEAN DEFAULT FALSE,
    p_language VARCHAR DEFAULT NULL,
    p_order_by VARCHAR DEFAULT 'relevance',  -- 'relevance', 'title', 'date', 'price'
    p_limit INTEGER DEFAULT 50,
    p_offset INTEGER DEFAULT 0
) RETURNS TABLE (
    id BIGINT,
    isbn VARCHAR,
    title VARCHAR,
    subtitle VARCHAR,
    author VARCHAR,
    publisher VARCHAR,
    publish_date DATE,
    price DECIMAL,
    language VARCHAR,
    available_copies INTEGER,
    total_copies INTEGER,
    score REAL
) AS $$
DECLARE
    v_sql TEXT;
    v_where_conditions TEXT[] := ARRAY[]::TEXT[];
    v_order_clause TEXT;
BEGIN
    -- Base query
    v_sql := '
        SELECT
            b.id,
            b.isbn,
            b.title,
            b.subtitle,
            b.author,
            b.publisher,
            b.publish_date,
            b.price,
            b.language,
            b.available_copies,
            b.total_copies,';

    -- Add score calculation
    IF p_query IS NOT NULL AND p_query != '' THEN
        v_sql := v_sql || '
            ts_rank(b.search_vector, plainto_tsquery(''simple'', $1)) AS score';
        v_where_conditions := array_append(v_where_conditions,
            'b.search_vector @@ plainto_tsquery(''simple'', $1)');
    ELSE
        v_sql := v_sql || '
            1.0::REAL AS score';
    END IF;

    v_sql := v_sql || '
        FROM books b';

    -- Join with category if needed
    IF p_category_id IS NOT NULL THEN
        v_sql := v_sql || '
        JOIN book_category_mapping bcm ON b.id = bcm.book_id';
        v_where_conditions := array_append(v_where_conditions,
            'bcm.category_id = $2');
    END IF;

    -- Add WHERE conditions
    v_where_conditions := array_append(v_where_conditions, 'b.deleted_at IS NULL');
    v_where_conditions := array_append(v_where_conditions, 'b.status = ''ACTIVE''');

    IF p_publisher IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'b.publisher ILIKE ''%'' || $3 || ''%''');
    END IF;

    IF p_publish_year_from IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'EXTRACT(YEAR FROM b.publish_date) >= $4');
    END IF;

    IF p_publish_year_to IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'EXTRACT(YEAR FROM b.publish_date) <= $5');
    END IF;

    IF p_price_min IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'b.price >= $6');
    END IF;

    IF p_price_max IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'b.price <= $7');
    END IF;

    IF p_available_only THEN
        v_where_conditions := array_append(v_where_conditions,
            'b.available_copies > 0');
    END IF;

    IF p_language IS NOT NULL THEN
        v_where_conditions := array_append(v_where_conditions,
            'b.language = $8');
    END IF;

    -- Build WHERE clause
    IF array_length(v_where_conditions, 1) > 0 THEN
        v_sql := v_sql || '
        WHERE ' || array_to_string(v_where_conditions, ' AND ');
    END IF;

    -- Add ORDER BY clause
    CASE p_order_by
        WHEN 'title' THEN
            v_order_clause := 'b.title ASC';
        WHEN 'date' THEN
            v_order_clause := 'b.publish_date DESC NULLS LAST';
        WHEN 'price' THEN
            v_order_clause := 'b.price ASC NULLS LAST';
        ELSE  -- 'relevance'
            IF p_query IS NOT NULL AND p_query != '' THEN
                v_order_clause := 'score DESC';
            ELSE
                v_order_clause := 'b.title ASC';
            END IF;
    END CASE;

    v_sql := v_sql || '
        ORDER BY ' || v_order_clause || '
        LIMIT $9 OFFSET $10';

    -- Execute dynamic SQL
    RETURN QUERY EXECUTE v_sql
        USING p_query, p_category_id, p_publisher,
              p_publish_year_from, p_publish_year_to,
              p_price_min, p_price_max, p_language,
              p_limit, p_offset;
END;
$$ LANGUAGE plpgsql;

-- Function to get search suggestions (autocomplete)
CREATE OR REPLACE FUNCTION get_search_suggestions(
    p_prefix TEXT,
    p_limit INTEGER DEFAULT 10
) RETURNS TABLE (
    suggestion TEXT,
    suggestion_type VARCHAR,
    frequency INTEGER
) AS $$
BEGIN
    -- Normalize prefix
    p_prefix := LOWER(TRIM(p_prefix));

    IF LENGTH(p_prefix) < 2 THEN
        RETURN;
    END IF;

    RETURN QUERY
    WITH suggestions AS (
        -- Title suggestions
        SELECT DISTINCT
            title AS suggestion,
            'title'::VARCHAR AS suggestion_type,
            COUNT(*) OVER (PARTITION BY title) AS frequency
        FROM books
        WHERE deleted_at IS NULL
            AND status = 'ACTIVE'
            AND LOWER(title) LIKE p_prefix || '%'

        UNION ALL

        -- Author suggestions
        SELECT DISTINCT
            author AS suggestion,
            'author'::VARCHAR AS suggestion_type,
            COUNT(*) OVER (PARTITION BY author) AS frequency
        FROM books
        WHERE deleted_at IS NULL
            AND status = 'ACTIVE'
            AND LOWER(author) LIKE p_prefix || '%'

        UNION ALL

        -- ISBN suggestions
        SELECT DISTINCT
            isbn AS suggestion,
            'isbn'::VARCHAR AS suggestion_type,
            1 AS frequency
        FROM books
        WHERE deleted_at IS NULL
            AND status = 'ACTIVE'
            AND isbn LIKE p_prefix || '%'
    )
    SELECT DISTINCT ON (s.suggestion)
        s.suggestion,
        s.suggestion_type,
        s.frequency
    FROM suggestions s
    ORDER BY s.suggestion, s.frequency DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 5. Triggers for Auto-Update
-- =========================================

-- Trigger to update search vector on insert or update
CREATE TRIGGER trg_update_book_search_vector
    BEFORE INSERT OR UPDATE OF
        title, subtitle, author, translator, publisher,
        isbn, subject_keywords, abstract, classification_code
    ON books
    FOR EACH ROW
    EXECUTE FUNCTION update_book_search_vector();

-- =========================================
-- 6. Update Existing Data
-- =========================================

-- Populate search vectors for existing books
UPDATE books
SET search_vector =
    setweight(to_tsvector('simple', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(subtitle, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(author, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(translator, '')), 'C') ||
    setweight(to_tsvector('simple', COALESCE(publisher, '')), 'C') ||
    setweight(to_tsvector('simple', COALESCE(isbn, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(subject_keywords, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(abstract, '')), 'D') ||
    setweight(to_tsvector('simple', COALESCE(classification_code, '')), 'C')
WHERE search_vector IS NULL;

-- =========================================
-- 7. Search Statistics Table (Optional)
-- =========================================

CREATE TABLE IF NOT EXISTS book_search_log (
    id BIGSERIAL PRIMARY KEY,
    search_query TEXT NOT NULL,
    search_type VARCHAR(20) NOT NULL,
    result_count INTEGER NOT NULL,
    execution_time_ms INTEGER,
    user_id BIGINT,
    session_id VARCHAR(100),
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_log_query ON book_search_log(search_query);
CREATE INDEX idx_search_log_created ON book_search_log(created_at DESC);
CREATE INDEX idx_search_log_user ON book_search_log(user_id);

-- Function to log search queries
CREATE OR REPLACE FUNCTION log_book_search(
    p_query TEXT,
    p_search_type VARCHAR,
    p_result_count INTEGER,
    p_execution_time_ms INTEGER,
    p_user_id BIGINT DEFAULT NULL,
    p_session_id VARCHAR DEFAULT NULL,
    p_ip_address VARCHAR DEFAULT NULL
) RETURNS VOID AS $$
BEGIN
    INSERT INTO book_search_log (
        search_query, search_type, result_count,
        execution_time_ms, user_id, session_id, ip_address
    ) VALUES (
        p_query, p_search_type, p_result_count,
        p_execution_time_ms, p_user_id, p_session_id, p_ip_address
    );
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 8. Views for Search Analytics
-- =========================================

-- Popular search terms
CREATE OR REPLACE VIEW v_popular_searches AS
SELECT
    search_query,
    COUNT(*) AS search_count,
    AVG(result_count) AS avg_results,
    AVG(execution_time_ms) AS avg_time_ms,
    MAX(created_at) AS last_searched
FROM book_search_log
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY search_query
HAVING COUNT(*) > 1
ORDER BY search_count DESC
LIMIT 100;

-- Search performance metrics
CREATE OR REPLACE VIEW v_search_performance AS
SELECT
    DATE(created_at) AS search_date,
    COUNT(*) AS total_searches,
    AVG(execution_time_ms) AS avg_response_time,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY execution_time_ms) AS median_time,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY execution_time_ms) AS p95_time,
    COUNT(DISTINCT user_id) AS unique_users,
    COUNT(*) FILTER (WHERE result_count = 0) AS zero_result_searches,
    ROUND((COUNT(*) FILTER (WHERE result_count = 0)::NUMERIC / COUNT(*)) * 100, 2) AS zero_result_rate
FROM book_search_log
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY search_date DESC;

-- =========================================
-- 9. Comments
-- =========================================

COMMENT ON COLUMN books.search_vector IS '全文搜索向量，包含加权的文本内容';
COMMENT ON COLUMN books.search_document IS '搜索文档，包含所有可搜索的文本字段';

COMMENT ON FUNCTION search_books IS '图书搜索主函数，支持精确、模糊和智能搜索';
COMMENT ON FUNCTION search_books_fuzzy IS '基于三元组的模糊搜索';
COMMENT ON FUNCTION search_books_advanced IS '高级搜索，支持多条件筛选';
COMMENT ON FUNCTION get_search_suggestions IS '搜索建议/自动补全功能';
COMMENT ON FUNCTION update_book_search_vector IS '更新搜索向量的触发器函数';

COMMENT ON TABLE book_search_log IS '图书搜索日志表，用于分析和优化';
COMMENT ON VIEW v_popular_searches IS '热门搜索词统计视图';
COMMENT ON VIEW v_search_performance IS '搜索性能指标视图';

-- =========================================
-- Migration completed successfully
-- Modified: 1 table (books - added 2 columns)
-- New Tables: 1 (book_search_log)
-- Indexes: 5 GIN indexes for full-text search
-- Functions: 6
-- Triggers: 1
-- Views: 2
-- Text Search Configuration: 1
-- =========================================