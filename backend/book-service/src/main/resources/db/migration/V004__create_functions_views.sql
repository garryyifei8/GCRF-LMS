-- =========================================
-- V004: Additional Functions, Views, and File Storage Fields
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- =========================================
-- 1. Add File Storage Fields to Books Table
-- =========================================

-- Add file storage related columns
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_url VARCHAR(500);
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_file_name VARCHAR(255);
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_file_size BIGINT;
ALTER TABLE books ADD COLUMN IF NOT EXISTS pdf_uploaded_at TIMESTAMP;
ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_file_name VARCHAR(255);
ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_file_size BIGINT;
ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_uploaded_at TIMESTAMP;

-- Add columns for multiple file attachments (future extension)
ALTER TABLE books ADD COLUMN IF NOT EXISTS attachment_count INTEGER DEFAULT 0;
ALTER TABLE books ADD COLUMN IF NOT EXISTS last_download_at TIMESTAMP;
ALTER TABLE books ADD COLUMN IF NOT EXISTS download_count BIGINT DEFAULT 0;

-- =========================================
-- 2. Create Book Attachments Table
-- =========================================

CREATE TABLE IF NOT EXISTS book_attachments (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    checksum VARCHAR(64),  -- SHA-256 hash
    description TEXT,
    upload_user_id BIGINT,
    upload_user_name VARCHAR(100),
    download_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_attachment_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_file_type CHECK (file_type IN (
        'COVER',      -- 封面图片
        'PDF',        -- PDF电子书
        'EPUB',       -- EPUB电子书
        'PREVIEW',    -- 预览文档
        'TOC',        -- 目录
        'SAMPLE',     -- 样章
        'AUDIO',      -- 音频文件
        'VIDEO',      -- 视频文件
        'OTHER'       -- 其他
    ))
);

-- Create indexes
CREATE INDEX idx_attachments_book_id ON book_attachments(book_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_attachments_file_type ON book_attachments(file_type)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_attachments_checksum ON book_attachments(checksum)
    WHERE deleted_at IS NULL;

-- =========================================
-- 3. Analytical Functions
-- =========================================

-- Function to get book statistics by category
CREATE OR REPLACE FUNCTION get_book_statistics_by_category(
    p_category_id BIGINT DEFAULT NULL,
    p_include_children BOOLEAN DEFAULT TRUE
) RETURNS TABLE (
    category_id BIGINT,
    category_name VARCHAR,
    category_path VARCHAR,
    total_books BIGINT,
    total_copies BIGINT,
    available_copies BIGINT,
    borrowed_copies BIGINT,
    reserved_copies BIGINT,
    avg_price NUMERIC(10,2),
    newest_book_date DATE,
    utilization_rate NUMERIC(5,2)
) AS $$
BEGIN
    IF p_category_id IS NULL THEN
        -- Statistics for all categories
        RETURN QUERY
        SELECT
            c.id AS category_id,
            c.category_name,
            c.path AS category_path,
            COUNT(DISTINCT bcm.book_id)::BIGINT AS total_books,
            SUM(b.total_copies)::BIGINT AS total_copies,
            SUM(b.available_copies)::BIGINT AS available_copies,
            SUM(b.borrowed_copies)::BIGINT AS borrowed_copies,
            SUM(b.reserved_copies)::BIGINT AS reserved_copies,
            ROUND(AVG(b.price), 2) AS avg_price,
            MAX(b.publish_date) AS newest_book_date,
            CASE
                WHEN SUM(b.total_copies) > 0 THEN
                    ROUND((SUM(b.borrowed_copies)::NUMERIC / SUM(b.total_copies)) * 100, 2)
                ELSE 0
            END AS utilization_rate
        FROM book_category c
        LEFT JOIN book_category_mapping bcm ON c.id = bcm.category_id
        LEFT JOIN books b ON bcm.book_id = b.id AND b.deleted_at IS NULL
        WHERE c.deleted_at IS NULL
        GROUP BY c.id, c.category_name, c.path
        ORDER BY c.path;
    ELSE
        -- Statistics for specific category
        IF p_include_children THEN
            -- Include child categories
            RETURN QUERY
            WITH target_categories AS (
                SELECT c.id
                FROM book_category c
                WHERE c.deleted_at IS NULL
                    AND (c.id = p_category_id OR c.path LIKE
                        (SELECT path FROM book_category WHERE id = p_category_id) || '.%')
            )
            SELECT
                p_category_id AS category_id,
                (SELECT category_name FROM book_category WHERE id = p_category_id) AS category_name,
                (SELECT path FROM book_category WHERE id = p_category_id) AS category_path,
                COUNT(DISTINCT bcm.book_id)::BIGINT AS total_books,
                SUM(b.total_copies)::BIGINT AS total_copies,
                SUM(b.available_copies)::BIGINT AS available_copies,
                SUM(b.borrowed_copies)::BIGINT AS borrowed_copies,
                SUM(b.reserved_copies)::BIGINT AS reserved_copies,
                ROUND(AVG(b.price), 2) AS avg_price,
                MAX(b.publish_date) AS newest_book_date,
                CASE
                    WHEN SUM(b.total_copies) > 0 THEN
                        ROUND((SUM(b.borrowed_copies)::NUMERIC / SUM(b.total_copies)) * 100, 2)
                    ELSE 0
                END AS utilization_rate
            FROM target_categories tc
            JOIN book_category_mapping bcm ON tc.id = bcm.category_id
            JOIN books b ON bcm.book_id = b.id
            WHERE b.deleted_at IS NULL;
        ELSE
            -- Only the specific category
            RETURN QUERY
            SELECT
                c.id AS category_id,
                c.category_name,
                c.path AS category_path,
                COUNT(DISTINCT bcm.book_id)::BIGINT AS total_books,
                SUM(b.total_copies)::BIGINT AS total_copies,
                SUM(b.available_copies)::BIGINT AS available_copies,
                SUM(b.borrowed_copies)::BIGINT AS borrowed_copies,
                SUM(b.reserved_copies)::BIGINT AS reserved_copies,
                ROUND(AVG(b.price), 2) AS avg_price,
                MAX(b.publish_date) AS newest_book_date,
                CASE
                    WHEN SUM(b.total_copies) > 0 THEN
                        ROUND((SUM(b.borrowed_copies)::NUMERIC / SUM(b.total_copies)) * 100, 2)
                    ELSE 0
                END AS utilization_rate
            FROM book_category c
            LEFT JOIN book_category_mapping bcm ON c.id = bcm.category_id
            LEFT JOIN books b ON bcm.book_id = b.id AND b.deleted_at IS NULL
            WHERE c.id = p_category_id AND c.deleted_at IS NULL
            GROUP BY c.id, c.category_name, c.path;
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to get popular books (most borrowed)
CREATE OR REPLACE FUNCTION get_popular_books(
    p_days INTEGER DEFAULT 30,
    p_category_id BIGINT DEFAULT NULL,
    p_limit INTEGER DEFAULT 10
) RETURNS TABLE (
    book_id BIGINT,
    isbn VARCHAR,
    title VARCHAR,
    author VARCHAR,
    borrow_count BIGINT,
    reserve_count BIGINT,
    total_circulation BIGINT,
    avg_rating NUMERIC(3,2),
    category_names TEXT
) AS $$
BEGIN
    RETURN QUERY
    WITH book_stats AS (
        SELECT
            b.id AS book_id,
            COUNT(DISTINCT bil.id) FILTER (WHERE bil.transaction_type = 'BORROW') AS borrow_count,
            COUNT(DISTINCT bil.id) FILTER (WHERE bil.transaction_type = 'RESERVE') AS reserve_count
        FROM books b
        LEFT JOIN book_inventory_log bil ON b.id = bil.book_id
            AND bil.created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day' * p_days
        WHERE b.deleted_at IS NULL
            AND b.status = 'ACTIVE'
        GROUP BY b.id
    ),
    book_categories AS (
        SELECT
            bcm.book_id,
            STRING_AGG(c.category_name, ', ' ORDER BY c.category_name) AS category_names
        FROM book_category_mapping bcm
        JOIN book_category c ON bcm.category_id = c.id
        WHERE c.deleted_at IS NULL
        GROUP BY bcm.book_id
    )
    SELECT
        b.id AS book_id,
        b.isbn,
        b.title,
        b.author,
        bs.borrow_count,
        bs.reserve_count,
        bs.borrow_count + bs.reserve_count AS total_circulation,
        NULL::NUMERIC(3,2) AS avg_rating,  -- Placeholder for future rating system
        bc.category_names
    FROM books b
    JOIN book_stats bs ON b.id = bs.book_id
    LEFT JOIN book_categories bc ON b.id = bc.book_id
    WHERE (p_category_id IS NULL OR EXISTS (
        SELECT 1 FROM book_category_mapping bcm
        WHERE bcm.book_id = b.id AND bcm.category_id = p_category_id
    ))
    ORDER BY total_circulation DESC, b.title
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze book acquisition trends
CREATE OR REPLACE FUNCTION analyze_acquisition_trends(
    p_months INTEGER DEFAULT 12
) RETURNS TABLE (
    month DATE,
    books_added BIGINT,
    copies_added BIGINT,
    total_value NUMERIC(12,2),
    publishers_count BIGINT,
    avg_price NUMERIC(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        DATE_TRUNC('month', b.created_at)::DATE AS month,
        COUNT(DISTINCT b.id)::BIGINT AS books_added,
        SUM(b.total_copies)::BIGINT AS copies_added,
        SUM(b.price * b.total_copies) AS total_value,
        COUNT(DISTINCT b.publisher)::BIGINT AS publishers_count,
        ROUND(AVG(b.price), 2) AS avg_price
    FROM books b
    WHERE b.created_at >= CURRENT_DATE - INTERVAL '1 month' * p_months
        AND b.deleted_at IS NULL
    GROUP BY DATE_TRUNC('month', b.created_at)
    ORDER BY month DESC;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 4. Comprehensive Views
-- =========================================

-- View for book details with all related information
CREATE OR REPLACE VIEW v_book_details AS
SELECT
    b.id,
    b.isbn,
    b.title,
    b.subtitle,
    b.author,
    b.translator,
    b.publisher,
    b.publish_date,
    b.edition,
    b.pages,
    b.price,
    b.binding,
    b.language,
    b.classification_code,
    b.subject_keywords,
    b.abstract,
    b.cover_url,
    b.pdf_url,
    b.pdf_file_name,
    b.pdf_file_size,
    b.total_copies,
    b.available_copies,
    b.borrowed_copies,
    b.reserved_copies,
    ROUND((b.borrowed_copies::NUMERIC / NULLIF(b.total_copies, 0)) * 100, 2) AS utilization_rate,
    b.status,
    b.version,
    b.created_at,
    b.updated_at,
    -- Category information
    (
        SELECT STRING_AGG(c.category_name, ', ' ORDER BY bcm.is_primary DESC, c.category_name)
        FROM book_category_mapping bcm
        JOIN book_category c ON bcm.category_id = c.id
        WHERE bcm.book_id = b.id AND c.deleted_at IS NULL
    ) AS categories,
    -- Attachment count
    (
        SELECT COUNT(*)
        FROM book_attachments ba
        WHERE ba.book_id = b.id AND ba.deleted_at IS NULL
    ) AS attachment_count,
    -- Stock status
    CASE
        WHEN b.available_copies = 0 THEN 'OUT_OF_STOCK'
        WHEN b.available_copies < 2 THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM books b
WHERE b.deleted_at IS NULL;

-- View for category hierarchy with statistics
CREATE OR REPLACE VIEW v_category_hierarchy AS
WITH RECURSIVE category_tree AS (
    SELECT
        c.id,
        c.parent_id,
        c.category_name,
        c.category_code,
        c.path,
        c.level,
        c.category_name AS full_path,
        c.book_count,
        c.child_count,
        c.sort_order,
        c.status,
        c.icon,
        c.color
    FROM book_category c
    WHERE c.parent_id IS NULL AND c.deleted_at IS NULL

    UNION ALL

    SELECT
        c.id,
        c.parent_id,
        c.category_name,
        c.category_code,
        c.path,
        c.level,
        ct.full_path || ' > ' || c.category_name AS full_path,
        c.book_count,
        c.child_count,
        c.sort_order,
        c.status,
        c.icon,
        c.color
    FROM book_category c
    INNER JOIN category_tree ct ON c.parent_id = ct.id
    WHERE c.deleted_at IS NULL
)
SELECT
    ct.*,
    -- Calculate total books including children
    (
        SELECT COUNT(DISTINCT bcm.book_id)
        FROM book_category_mapping bcm
        JOIN book_category c2 ON bcm.category_id = c2.id
        JOIN books b ON bcm.book_id = b.id
        WHERE b.deleted_at IS NULL
            AND c2.deleted_at IS NULL
            AND (c2.id = ct.id OR c2.path LIKE ct.path || '.%')
    ) AS total_books_recursive,
    -- Calculate direct children count
    (
        SELECT COUNT(*)
        FROM book_category c3
        WHERE c3.parent_id = ct.id AND c3.deleted_at IS NULL
    ) AS direct_children_count
FROM category_tree ct
ORDER BY ct.path;

-- View for daily circulation statistics
CREATE OR REPLACE VIEW v_daily_circulation_stats AS
SELECT
    DATE(bil.created_at) AS date,
    COUNT(*) FILTER (WHERE bil.transaction_type = 'BORROW') AS borrows,
    COUNT(*) FILTER (WHERE bil.transaction_type = 'RETURN') AS returns,
    COUNT(*) FILTER (WHERE bil.transaction_type = 'RESERVE') AS reserves,
    COUNT(*) FILTER (WHERE bil.transaction_type = 'CANCEL_RESERVE') AS cancelled_reserves,
    COUNT(*) FILTER (WHERE bil.transaction_type IN ('LOSS', 'DAMAGE', 'DISCARD')) AS losses,
    COUNT(*) FILTER (WHERE bil.transaction_type IN ('PURCHASE', 'DONATION')) AS acquisitions,
    COUNT(DISTINCT bil.book_id) AS unique_books,
    COUNT(DISTINCT bil.operator_id) AS unique_operators
FROM book_inventory_log bil
WHERE bil.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(bil.created_at)
ORDER BY date DESC;

-- View for publisher statistics
CREATE OR REPLACE VIEW v_publisher_statistics AS
SELECT
    b.publisher,
    COUNT(DISTINCT b.id) AS book_count,
    SUM(b.total_copies) AS total_copies,
    SUM(b.available_copies) AS available_copies,
    ROUND(AVG(b.price), 2) AS avg_price,
    MIN(b.publish_date) AS earliest_book,
    MAX(b.publish_date) AS latest_book,
    COUNT(DISTINCT b.author) AS author_count,
    COUNT(DISTINCT b.language) AS language_count
FROM books b
WHERE b.deleted_at IS NULL
    AND b.publisher IS NOT NULL
GROUP BY b.publisher
HAVING COUNT(*) > 1
ORDER BY book_count DESC;

-- =========================================
-- 5. Materialized Views for Performance
-- =========================================

-- Materialized view for book search (refresh periodically)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_book_search AS
SELECT
    b.id,
    b.isbn,
    b.title,
    b.subtitle,
    b.author,
    b.translator,
    b.publisher,
    b.publish_date,
    b.price,
    b.language,
    b.available_copies,
    b.total_copies,
    b.search_vector,
    b.status,
    STRING_AGG(DISTINCT c.category_name, ', ' ORDER BY c.category_name) AS categories,
    STRING_AGG(DISTINCT c.category_code, ', ' ORDER BY c.category_code) AS category_codes
FROM books b
LEFT JOIN book_category_mapping bcm ON b.id = bcm.book_id
LEFT JOIN book_category c ON bcm.category_id = c.id AND c.deleted_at IS NULL
WHERE b.deleted_at IS NULL
GROUP BY b.id;

-- Create indexes on materialized view
CREATE INDEX idx_mv_book_search_vector ON mv_book_search USING gin(search_vector);
CREATE INDEX idx_mv_book_search_isbn ON mv_book_search(isbn);
CREATE INDEX idx_mv_book_search_status ON mv_book_search(status);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_book_search_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_book_search;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 6. Utility Functions
-- =========================================

-- Function to check file duplicate by checksum
CREATE OR REPLACE FUNCTION check_file_duplicate(
    p_checksum VARCHAR
) RETURNS TABLE (
    book_id BIGINT,
    book_title VARCHAR,
    file_name VARCHAR,
    file_type VARCHAR,
    uploaded_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ba.book_id,
        b.title AS book_title,
        ba.file_name,
        ba.file_type,
        ba.created_at AS uploaded_at
    FROM book_attachments ba
    JOIN books b ON ba.book_id = b.id
    WHERE ba.checksum = p_checksum
        AND ba.deleted_at IS NULL
        AND b.deleted_at IS NULL
    ORDER BY ba.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to get file storage statistics
CREATE OR REPLACE FUNCTION get_file_storage_statistics()
RETURNS TABLE (
    total_files BIGINT,
    total_size_bytes BIGINT,
    total_size_mb NUMERIC(12,2),
    total_size_gb NUMERIC(12,2),
    avg_file_size_mb NUMERIC(12,2),
    files_by_type JSONB,
    top_downloaders JSONB
) AS $$
BEGIN
    RETURN QUERY
    WITH file_stats AS (
        SELECT
            COUNT(*) AS total_files,
            SUM(file_size) AS total_size_bytes,
            SUM(download_count) AS total_downloads
        FROM book_attachments
        WHERE deleted_at IS NULL
    ),
    type_stats AS (
        SELECT
            jsonb_object_agg(
                file_type,
                jsonb_build_object(
                    'count', file_count,
                    'size_mb', ROUND(total_size / 1024.0 / 1024.0, 2)
                )
            ) AS by_type
        FROM (
            SELECT
                file_type,
                COUNT(*) AS file_count,
                SUM(file_size) AS total_size
            FROM book_attachments
            WHERE deleted_at IS NULL
            GROUP BY file_type
        ) t
    ),
    top_downloads AS (
        SELECT
            jsonb_agg(
                jsonb_build_object(
                    'book_id', book_id,
                    'title', title,
                    'downloads', download_count
                ) ORDER BY download_count DESC
            ) AS top_10
        FROM (
            SELECT
                ba.book_id,
                b.title,
                SUM(ba.download_count) AS download_count
            FROM book_attachments ba
            JOIN books b ON ba.book_id = b.id
            WHERE ba.deleted_at IS NULL AND b.deleted_at IS NULL
            GROUP BY ba.book_id, b.title
            ORDER BY download_count DESC
            LIMIT 10
        ) t
    )
    SELECT
        fs.total_files,
        fs.total_size_bytes,
        ROUND(fs.total_size_bytes / 1024.0 / 1024.0, 2) AS total_size_mb,
        ROUND(fs.total_size_bytes / 1024.0 / 1024.0 / 1024.0, 2) AS total_size_gb,
        CASE
            WHEN fs.total_files > 0 THEN
                ROUND(fs.total_size_bytes / fs.total_files / 1024.0 / 1024.0, 2)
            ELSE 0
        END AS avg_file_size_mb,
        ts.by_type AS files_by_type,
        td.top_10 AS top_downloaders
    FROM file_stats fs
    CROSS JOIN type_stats ts
    CROSS JOIN top_downloads td;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 7. Comments
-- =========================================

COMMENT ON COLUMN books.pdf_url IS 'PDF文件的MinIO存储URL';
COMMENT ON COLUMN books.pdf_file_name IS 'PDF文件原始名称';
COMMENT ON COLUMN books.pdf_file_size IS 'PDF文件大小（字节）';
COMMENT ON COLUMN books.cover_file_name IS '封面图片原始名称';
COMMENT ON COLUMN books.attachment_count IS '附件总数';
COMMENT ON COLUMN books.download_count IS '下载总次数';

COMMENT ON TABLE book_attachments IS '图书附件表，存储所有相关文件';
COMMENT ON COLUMN book_attachments.file_type IS '文件类型：COVER封面, PDF电子书等';
COMMENT ON COLUMN book_attachments.checksum IS '文件SHA-256校验和，用于去重';

COMMENT ON FUNCTION get_book_statistics_by_category IS '获取分类图书统计信息';
COMMENT ON FUNCTION get_popular_books IS '获取热门图书排行';
COMMENT ON FUNCTION analyze_acquisition_trends IS '分析图书采购趋势';
COMMENT ON FUNCTION check_file_duplicate IS '通过校验和检查文件重复';
COMMENT ON FUNCTION get_file_storage_statistics IS '获取文件存储统计信息';

COMMENT ON VIEW v_book_details IS '图书详细信息视图，包含所有相关数据';
COMMENT ON VIEW v_category_hierarchy IS '分类层级结构视图，包含递归统计';
COMMENT ON VIEW v_daily_circulation_stats IS '每日流通统计视图';
COMMENT ON VIEW v_publisher_statistics IS '出版社统计视图';

COMMENT ON MATERIALIZED VIEW mv_book_search IS '图书搜索物化视图，提升搜索性能';

-- =========================================
-- Migration completed successfully
-- Modified: 1 table (books - added 10 columns)
-- New Tables: 1 (book_attachments)
-- Functions: 8
-- Views: 5
-- Materialized Views: 1
-- Indexes: 6
-- =========================================