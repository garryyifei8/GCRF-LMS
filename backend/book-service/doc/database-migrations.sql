-- =====================================================
-- Book Service Database Migration Scripts
-- Version: 1.0.0
-- Date: 2025-11-03
-- Description: Complete database schema for Book Service
--              including category management, full-text search,
--              file storage, and advanced inventory tracking
-- =====================================================

-- Set timezone and encoding
SET TIME ZONE 'Asia/Shanghai';
SET client_encoding = 'UTF8';

-- =====================================================
-- PART 1: Extensions
-- =====================================================

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";     -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- Trigram similarity search
CREATE EXTENSION IF NOT EXISTS "btree_gin";     -- GIN index support for btree
CREATE EXTENSION IF NOT EXISTS "zhparser";      -- Chinese text parsing (optional, if available)

-- =====================================================
-- PART 2: Book Category Management
-- =====================================================

-- Drop existing tables if needed (for clean migration)
DROP TABLE IF EXISTS book_category_relation CASCADE;
DROP TABLE IF EXISTS book_category CASCADE;

-- Create book category table with hierarchical structure
CREATE TABLE book_category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES book_category(id) ON DELETE CASCADE,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) UNIQUE NOT NULL,
    category_path VARCHAR(500), -- Materialized path for efficient queries
    description TEXT,
    icon_url VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    is_leaf BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for performance
    INDEX idx_category_parent_id (parent_id),
    INDEX idx_category_code (category_code),
    INDEX idx_category_path (category_path),
    INDEX idx_category_status (status)
);

-- Create many-to-many relationship table
CREATE TABLE book_category_relation (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES book_category(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT FALSE, -- Primary category for the book
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Composite unique constraint
    UNIQUE(book_id, category_id),

    -- Indexes for queries
    INDEX idx_book_category_book_id (book_id),
    INDEX idx_book_category_category_id (category_id),
    INDEX idx_book_category_primary (book_id, is_primary) WHERE is_primary = TRUE
);

-- Function to update category path
CREATE OR REPLACE FUNCTION update_category_path() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NULL THEN
        NEW.category_path = '/' || NEW.id || '/';
        NEW.level = 1;
    ELSE
        SELECT category_path || NEW.id || '/', level + 1
        INTO NEW.category_path, NEW.level
        FROM book_category
        WHERE id = NEW.parent_id;
    END IF;

    -- Update parent's is_leaf status
    IF NEW.parent_id IS NOT NULL THEN
        UPDATE book_category SET is_leaf = FALSE WHERE id = NEW.parent_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to maintain category path
CREATE TRIGGER trig_update_category_path
    BEFORE INSERT OR UPDATE OF parent_id ON book_category
    FOR EACH ROW EXECUTE FUNCTION update_category_path();

-- Insert seed data for categories
INSERT INTO book_category (category_name, category_code, description, sort_order) VALUES
    ('文学', 'literature', '包括小说、诗歌、散文等文学作品', 1),
    ('科技', 'technology', '科学技术类图书', 2),
    ('历史', 'history', '历史、地理、传记类图书', 3),
    ('艺术', 'art', '艺术、音乐、绘画类图书', 4),
    ('教育', 'education', '教材、教辅、考试用书', 5),
    ('经济管理', 'business', '经济、管理、投资类图书', 6),
    ('生活', 'lifestyle', '生活、健康、旅游类图书', 7),
    ('儿童', 'children', '少儿读物、绘本、童话', 8),
    ('社会科学', 'social_science', '哲学、心理学、社会学', 9),
    ('计算机', 'computer', '编程、软件、网络技术', 10);

-- Insert sub-categories
INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '中国文学', 'chinese_literature', '中国古代及现当代文学作品', 1
FROM book_category WHERE category_code = 'literature';

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '外国文学', 'foreign_literature', '外国文学作品及译著', 2
FROM book_category WHERE category_code = 'literature';

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '编程语言', 'programming', 'Java、Python、Go等编程语言', 1
FROM book_category WHERE category_code = 'computer';

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '数据库', 'database', '数据库原理与应用', 2
FROM book_category WHERE category_code = 'computer';

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '人工智能', 'ai', '机器学习、深度学习、AI应用', 3
FROM book_category WHERE category_code = 'computer';

-- =====================================================
-- PART 3: Book Table Extensions
-- =====================================================

-- Add file storage fields to book table
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_url VARCHAR(500);
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_file_name VARCHAR(255);
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_file_size BIGINT;
ALTER TABLE books ADD COLUMN IF NOT EXISTS file_upload_time TIMESTAMP;
ALTER TABLE books ADD COLUMN IF NOT EXISTS file_version INTEGER DEFAULT 1;

-- Add inventory tracking fields
ALTER TABLE books ADD COLUMN IF NOT EXISTS total_copies INTEGER DEFAULT 0;
ALTER TABLE books ADD COLUMN IF NOT EXISTS borrowed_copies INTEGER DEFAULT 0;
ALTER TABLE books ADD COLUMN IF NOT EXISTS reserved_copies INTEGER DEFAULT 0;

-- Add computed column for available copies
ALTER TABLE books ADD COLUMN IF NOT EXISTS available_copies_computed
    INTEGER GENERATED ALWAYS AS (total_copies - borrowed_copies - reserved_copies) STORED;

-- Add check constraint for inventory consistency
ALTER TABLE books ADD CONSTRAINT chk_book_inventory
    CHECK (
        total_copies >= 0 AND
        borrowed_copies >= 0 AND
        reserved_copies >= 0 AND
        (borrowed_copies + reserved_copies) <= total_copies
    );

-- Add optimistic locking version
ALTER TABLE books ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- =====================================================
-- PART 4: Full-Text Search Support
-- =====================================================

-- Add search vector column
ALTER TABLE books ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update search vector with Chinese support
CREATE OR REPLACE FUNCTION book_search_vector_update() RETURNS trigger AS $$
BEGIN
    -- Build search vector with weighted fields
    NEW.search_vector :=
        setweight(to_tsvector('simple', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.subtitle, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.author, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.isbn, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.publisher, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.subject_keywords, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.abstract, '')), 'D');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update search vector
DROP TRIGGER IF EXISTS trig_book_search_vector_update ON books;
CREATE TRIGGER trig_book_search_vector_update
    BEFORE INSERT OR UPDATE OF title, subtitle, author, isbn, publisher, subject_keywords, abstract
    ON books
    FOR EACH ROW EXECUTE FUNCTION book_search_vector_update();

-- Update existing records' search vectors
UPDATE books SET search_vector = search_vector WHERE search_vector IS NULL;

-- =====================================================
-- PART 5: Indexes for Performance
-- =====================================================

-- Drop existing indexes if they exist
DROP INDEX IF EXISTS idx_book_search;
DROP INDEX IF EXISTS idx_book_title_trgm;
DROP INDEX IF EXISTS idx_book_author_trgm;
DROP INDEX IF EXISTS idx_book_isbn;
DROP INDEX IF EXISTS idx_book_status;
DROP INDEX IF EXISTS idx_book_available;
DROP INDEX IF EXISTS idx_book_created;

-- Full-text search index (GIN index for tsvector)
CREATE INDEX idx_book_search ON books USING gin(search_vector);

-- Trigram indexes for fuzzy search
CREATE INDEX idx_book_title_trgm ON books USING gin(title gin_trgm_ops);
CREATE INDEX idx_book_author_trgm ON books USING gin(author gin_trgm_ops);
CREATE INDEX idx_book_publisher_trgm ON books USING gin(publisher gin_trgm_ops);

-- Regular indexes for common queries
CREATE INDEX idx_book_isbn ON books(isbn) WHERE deleted_at IS NULL;
CREATE INDEX idx_book_status ON books(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_book_classification ON books(classification_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_book_available ON books(available_copies_computed) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX idx_book_created ON books(created_at DESC) WHERE deleted_at IS NULL;

-- Composite indexes for complex queries
CREATE INDEX idx_book_status_available ON books(status, available_copies_computed)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_book_category_status ON book_category_relation(category_id, book_id)
    INCLUDE (is_primary);

-- =====================================================
-- PART 6: Inventory Tracking Tables
-- =====================================================

-- Create inventory transaction log table
CREATE TABLE IF NOT EXISTS book_inventory_log (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN (
        'BORROW', 'RETURN', 'RESERVE', 'CANCEL_RESERVE',
        'ADD_STOCK', 'REMOVE_STOCK', 'DAMAGE', 'LOST'
    )),
    quantity INTEGER NOT NULL,
    before_total INTEGER NOT NULL,
    after_total INTEGER NOT NULL,
    before_available INTEGER NOT NULL,
    after_available INTEGER NOT NULL,
    operator_id BIGINT,
    operator_name VARCHAR(100),
    reason TEXT,
    related_id BIGINT, -- Related circulation_id or reservation_id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_inventory_log_book_id (book_id),
    INDEX idx_inventory_log_type (transaction_type),
    INDEX idx_inventory_log_created (created_at DESC)
);

-- Function to log inventory changes
CREATE OR REPLACE FUNCTION log_inventory_change() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.total_copies != NEW.total_copies OR
       OLD.available_quantity != NEW.available_quantity OR
       OLD.borrowed_copies != NEW.borrowed_copies OR
       OLD.reserved_copies != NEW.reserved_copies THEN

        INSERT INTO book_inventory_log (
            book_id,
            transaction_type,
            quantity,
            before_total,
            after_total,
            before_available,
            after_available,
            created_at
        ) VALUES (
            NEW.id,
            CASE
                WHEN NEW.borrowed_copies > OLD.borrowed_copies THEN 'BORROW'
                WHEN NEW.borrowed_copies < OLD.borrowed_copies THEN 'RETURN'
                WHEN NEW.reserved_copies > OLD.reserved_copies THEN 'RESERVE'
                WHEN NEW.reserved_copies < OLD.reserved_copies THEN 'CANCEL_RESERVE'
                WHEN NEW.total_copies > OLD.total_copies THEN 'ADD_STOCK'
                WHEN NEW.total_copies < OLD.total_copies THEN 'REMOVE_STOCK'
                ELSE 'UPDATE'
            END,
            ABS(NEW.total_copies - OLD.total_copies),
            OLD.total_copies,
            NEW.total_copies,
            OLD.available_quantity,
            NEW.available_quantity,
            CURRENT_TIMESTAMP
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for inventory logging
CREATE TRIGGER trig_log_inventory_change
    AFTER UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION log_inventory_change();

-- =====================================================
-- PART 7: File Upload Audit Table
-- =====================================================

CREATE TABLE IF NOT EXISTS book_file_uploads (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('COVER', 'PDF', 'PREVIEW')),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    upload_user_id BIGINT,
    upload_user_name VARCHAR(100),
    upload_ip VARCHAR(45),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DELETED', 'REPLACED')),
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_file_upload_book_id (book_id),
    INDEX idx_file_upload_type (file_type),
    INDEX idx_file_upload_status (status),
    INDEX idx_file_upload_created (created_at DESC)
);

-- =====================================================
-- PART 8: Search History and Analytics
-- =====================================================

CREATE TABLE IF NOT EXISTS book_search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    search_query TEXT NOT NULL,
    search_type VARCHAR(20) DEFAULT 'KEYWORD', -- KEYWORD, ADVANCED, CATEGORY, ISBN
    result_count INTEGER DEFAULT 0,
    selected_book_id BIGINT,
    search_filters JSONB, -- Store advanced filters as JSON
    session_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for analytics
    INDEX idx_search_history_user_id (user_id),
    INDEX idx_search_history_query (search_query),
    INDEX idx_search_history_created (created_at DESC)
);

-- Popular searches materialized view
CREATE MATERIALIZED VIEW popular_searches AS
SELECT
    search_query,
    COUNT(*) as search_count,
    AVG(result_count) as avg_results,
    COUNT(DISTINCT user_id) as unique_users,
    MAX(created_at) as last_searched
FROM book_search_history
WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
GROUP BY search_query
HAVING COUNT(*) > 5
ORDER BY search_count DESC
LIMIT 100;

-- Refresh popular searches daily
CREATE INDEX idx_popular_searches_count ON popular_searches(search_count DESC);

-- =====================================================
-- PART 9: Functions and Stored Procedures
-- =====================================================

-- Function to search books with full-text search
CREATE OR REPLACE FUNCTION search_books(
    p_query TEXT,
    p_category_id BIGINT DEFAULT NULL,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
) RETURNS TABLE (
    book_id BIGINT,
    title VARCHAR,
    author VARCHAR,
    isbn VARCHAR,
    publisher VARCHAR,
    cover_url VARCHAR,
    available_copies INTEGER,
    score REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        b.id,
        b.title,
        b.author,
        b.isbn,
        b.publisher,
        b.cover_url,
        b.available_copies_computed,
        ts_rank(b.search_vector, plainto_tsquery('simple', p_query)) as score
    FROM books b
    LEFT JOIN book_category_relation bcr ON b.id = bcr.book_id
    WHERE
        b.deleted_at IS NULL
        AND b.status = 'ACTIVE'
        AND (p_query IS NULL OR b.search_vector @@ plainto_tsquery('simple', p_query))
        AND (p_category_id IS NULL OR bcr.category_id = p_category_id)
    ORDER BY score DESC, b.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Function to get book with categories
CREATE OR REPLACE FUNCTION get_book_with_categories(p_book_id BIGINT)
RETURNS TABLE (
    book_id BIGINT,
    title VARCHAR,
    author VARCHAR,
    categories JSON
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        b.id,
        b.title,
        b.author,
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', c.id,
                'name', c.category_name,
                'code', c.category_code,
                'is_primary', bcr.is_primary
            ) ORDER BY bcr.is_primary DESC, c.sort_order
        ) as categories
    FROM books b
    LEFT JOIN book_category_relation bcr ON b.id = bcr.book_id
    LEFT JOIN book_category c ON bcr.category_id = c.id
    WHERE b.id = p_book_id
    GROUP BY b.id, b.title, b.author;
END;
$$ LANGUAGE plpgsql;

-- Function to update inventory with concurrency control
CREATE OR REPLACE FUNCTION update_book_inventory(
    p_book_id BIGINT,
    p_operation VARCHAR,
    p_quantity INTEGER DEFAULT 1
) RETURNS BOOLEAN AS $$
DECLARE
    v_success BOOLEAN := FALSE;
BEGIN
    -- Use SELECT FOR UPDATE to lock the row
    UPDATE books
    SET
        borrowed_copies = CASE
            WHEN p_operation = 'BORROW' THEN borrowed_copies + p_quantity
            WHEN p_operation = 'RETURN' THEN borrowed_copies - p_quantity
            ELSE borrowed_copies
        END,
        reserved_copies = CASE
            WHEN p_operation = 'RESERVE' THEN reserved_copies + p_quantity
            WHEN p_operation = 'CANCEL_RESERVE' THEN reserved_copies - p_quantity
            ELSE reserved_copies
        END,
        version = version + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_book_id
    AND deleted_at IS NULL;

    GET DIAGNOSTICS v_success = ROW_COUNT > 0;

    RETURN v_success;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PART 10: Permissions and Security
-- =====================================================

-- Create read-only role for reporting
CREATE ROLE book_service_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO book_service_readonly;

-- Create application role with full permissions
CREATE ROLE book_service_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO book_service_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO book_service_app;

-- =====================================================
-- PART 11: Data Migration (if upgrading existing system)
-- =====================================================

-- Update existing books to use new inventory fields if needed
UPDATE books
SET
    total_copies = total_quantity,
    borrowed_copies = total_quantity - available_quantity,
    reserved_copies = 0
WHERE total_copies IS NULL;

-- Rebuild search vectors for all existing books
UPDATE books SET search_vector = search_vector WHERE true;

-- =====================================================
-- PART 12: Maintenance Scripts
-- =====================================================

-- Script to refresh materialized views (run daily)
CREATE OR REPLACE FUNCTION refresh_materialized_views() RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY popular_searches;
    -- Add other materialized views here
END;
$$ LANGUAGE plpgsql;

-- Script to clean up old data (run weekly)
CREATE OR REPLACE FUNCTION cleanup_old_data() RETURNS void AS $$
BEGIN
    -- Delete old search history (> 90 days)
    DELETE FROM book_search_history WHERE created_at < CURRENT_DATE - INTERVAL '90 days';

    -- Delete old inventory logs (> 1 year)
    DELETE FROM book_inventory_log WHERE created_at < CURRENT_DATE - INTERVAL '1 year';

    -- Clean up deleted file records (> 30 days)
    DELETE FROM book_file_uploads
    WHERE status = 'DELETED' AND deleted_at < CURRENT_DATE - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PART 13: Sample Test Data
-- =====================================================

-- Insert sample books with full-text search data
INSERT INTO books (
    isbn, title, subtitle, author, publisher,
    publish_date, pages, price, language,
    classification_code, subject_keywords, abstract,
    total_quantity, available_quantity, status
) VALUES
(
    '9787115551474',
    'Spring Boot实战',
    '从入门到精通',
    'Craig Walls',
    '人民邮电出版社',
    '2021-01-01',
    520,
    89.00,
    'zh-CN',
    'TP312',
    'Spring Boot,微服务,Java,Web开发',
    '本书是Spring Boot领域的经典著作，全面介绍了Spring Boot的核心特性和最佳实践。',
    10,
    8,
    'ACTIVE'
),
(
    '9787121394881',
    '微服务架构设计模式',
    NULL,
    'Chris Richardson',
    '电子工业出版社',
    '2020-09-01',
    448,
    108.00,
    'zh-CN',
    'TP311',
    '微服务,架构,设计模式,分布式',
    '本书详细阐述了微服务架构的设计模式和最佳实践，是微服务开发者的必备参考。',
    5,
    3,
    'ACTIVE'
);

-- Assign categories to sample books
INSERT INTO book_category_relation (book_id, category_id, is_primary)
SELECT
    b.id,
    c.id,
    TRUE
FROM books b, book_category c
WHERE b.isbn = '9787115551474' AND c.category_code = 'programming';

INSERT INTO book_category_relation (book_id, category_id, is_primary)
SELECT
    b.id,
    c.id,
    TRUE
FROM books b, book_category c
WHERE b.isbn = '9787121394881' AND c.category_code = 'programming';

-- =====================================================
-- PART 14: Rollback Script (Save for emergency)
-- =====================================================

/*
-- Rollback script (DO NOT RUN unless needed)

-- Remove triggers
DROP TRIGGER IF EXISTS trig_book_search_vector_update ON books;
DROP TRIGGER IF EXISTS trig_log_inventory_change ON books;
DROP TRIGGER IF EXISTS trig_update_category_path ON book_category;

-- Remove functions
DROP FUNCTION IF EXISTS book_search_vector_update() CASCADE;
DROP FUNCTION IF EXISTS log_inventory_change() CASCADE;
DROP FUNCTION IF EXISTS update_category_path() CASCADE;
DROP FUNCTION IF EXISTS search_books(TEXT, BIGINT, INTEGER, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS get_book_with_categories(BIGINT) CASCADE;
DROP FUNCTION IF EXISTS update_book_inventory(BIGINT, VARCHAR, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS refresh_materialized_views() CASCADE;
DROP FUNCTION IF EXISTS cleanup_old_data() CASCADE;

-- Remove materialized views
DROP MATERIALIZED VIEW IF EXISTS popular_searches CASCADE;

-- Remove tables
DROP TABLE IF EXISTS book_search_history CASCADE;
DROP TABLE IF EXISTS book_file_uploads CASCADE;
DROP TABLE IF EXISTS book_inventory_log CASCADE;
DROP TABLE IF EXISTS book_category_relation CASCADE;
DROP TABLE IF EXISTS book_category CASCADE;

-- Remove columns from books table
ALTER TABLE books DROP COLUMN IF EXISTS search_vector;
ALTER TABLE books DROP COLUMN IF EXISTS pdf_url;
ALTER TABLE books DROP COLUMN IF EXISTS pdf_file_name;
ALTER TABLE books DROP COLUMN IF EXISTS pdf_file_size;
ALTER TABLE books DROP COLUMN IF EXISTS file_upload_time;
ALTER TABLE books DROP COLUMN IF EXISTS file_version;
ALTER TABLE books DROP COLUMN IF EXISTS total_copies;
ALTER TABLE books DROP COLUMN IF EXISTS borrowed_copies;
ALTER TABLE books DROP COLUMN IF EXISTS reserved_copies;
ALTER TABLE books DROP COLUMN IF EXISTS available_copies_computed;
ALTER TABLE books DROP COLUMN IF EXISTS version;

-- Remove constraints
ALTER TABLE books DROP CONSTRAINT IF EXISTS chk_book_inventory;

-- Remove indexes
DROP INDEX IF EXISTS idx_book_search;
DROP INDEX IF EXISTS idx_book_title_trgm;
DROP INDEX IF EXISTS idx_book_author_trgm;
DROP INDEX IF EXISTS idx_book_publisher_trgm;

-- Remove roles
DROP ROLE IF EXISTS book_service_readonly;
DROP ROLE IF EXISTS book_service_app;

*/

-- =====================================================
-- END OF MIGRATION SCRIPT
-- =====================================================

-- Display completion message
DO $$
BEGIN
    RAISE NOTICE 'Book Service database migration completed successfully!';
    RAISE NOTICE 'Tables created: book_category, book_category_relation, book_inventory_log, book_file_uploads, book_search_history';
    RAISE NOTICE 'Indexes created: Full-text search and performance indexes';
    RAISE NOTICE 'Functions created: Search and inventory management functions';
    RAISE NOTICE 'Please run ANALYZE to update statistics: ANALYZE books;';
END $$;