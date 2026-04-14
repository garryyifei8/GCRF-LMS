-- =========================================
-- V005: Seed Data for Book Categories and Sample Books
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- =========================================
-- 1. Insert Book Categories (Chinese Library Classification)
-- =========================================

-- Level 1: Main Categories
INSERT INTO book_category (parent_id, category_name, category_code, description, icon, color, sort_order)
VALUES
    (NULL, '马克思主义、列宁主义、毛泽东思想、邓小平理论', 'A', '马列主义理论著作', 'BookOpen', '#FF6B6B', 1),
    (NULL, '哲学、宗教', 'B', '哲学理论、宗教研究', 'Yin-yang', '#4ECDC4', 2),
    (NULL, '社会科学总论', 'C', '社会科学理论与方法', 'Users', '#45B7D1', 3),
    (NULL, '政治、法律', 'D', '政治理论、法律法规', 'Scale', '#96CEB4', 4),
    (NULL, '军事', 'E', '军事理论与军事技术', 'Shield', '#FFEAA7', 5),
    (NULL, '经济', 'F', '经济理论与经济管理', 'TrendingUp', '#DDA0DD', 6),
    (NULL, '文化、科学、教育、体育', 'G', '文化教育体育事业', 'GraduationCap', '#98D8C8', 7),
    (NULL, '语言、文字', 'H', '语言学、文字学', 'MessageSquare', '#F7B267', 8),
    (NULL, '文学', 'I', '各类文学作品', 'BookHeart', '#F06292', 9),
    (NULL, '艺术', 'J', '艺术理论与艺术作品', 'Palette', '#AED581', 10),
    (NULL, '历史、地理', 'K', '历史研究、地理学', 'Globe', '#FFD54F', 11),
    (NULL, '自然科学总论', 'N', '自然科学理论', 'Microscope', '#4FC3F7', 12),
    (NULL, '数理科学和化学', 'O', '数学、物理、化学', 'Calculator', '#9575CD', 13),
    (NULL, '天文学、地球科学', 'P', '天文、气象、地质', 'Star', '#4DB6AC', 14),
    (NULL, '生物科学', 'Q', '生物学各分支', 'Dna', '#81C784', 15),
    (NULL, '医药、卫生', 'R', '医学、药学、卫生学', 'Heart', '#E57373', 16),
    (NULL, '农业科学', 'S', '农业、林业、畜牧业', 'Leaf', '#A1C181', 17),
    (NULL, '工业技术', 'T', '各类工业技术', 'Wrench', '#78909C', 18),
    (NULL, '交通运输', 'U', '交通运输技术', 'Car', '#8D6E63', 19),
    (NULL, '航空、航天', 'V', '航空航天技术', 'Rocket', '#7E57C2', 20),
    (NULL, '环境科学、安全科学', 'X', '环境保护、安全生产', 'TreePine', '#66BB6A', 21),
    (NULL, '综合性图书', 'Z', '百科全书、年鉴等', 'Library', '#EC407A', 22)
ON CONFLICT (category_code) DO NOTHING;

-- Level 2: Selected Sub-categories
-- Literature subcategories
INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '中国文学', 'I2', '中国各时期文学作品', 1
FROM book_category WHERE category_code = 'I' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '外国文学', 'I7', '世界各国文学作品', 2
FROM book_category WHERE category_code = 'I' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

-- Computer Science subcategories
INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '计算机科学', 'TP3', '计算机硬件、软件及应用', 1
FROM book_category WHERE category_code = 'T' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

-- Level 3: Programming languages
INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, 'Java编程', 'TP312JA', 'Java语言程序设计', 1
FROM book_category WHERE category_code = 'TP3' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, 'Python编程', 'TP312PY', 'Python语言程序设计', 2
FROM book_category WHERE category_code = 'TP3' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '数据库技术', 'TP311DB', '数据库原理与应用', 3
FROM book_category WHERE category_code = 'TP3' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '人工智能', 'TP18', '人工智能与机器学习', 4
FROM book_category WHERE category_code = 'TP3' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

-- Economics subcategories
INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '经济学理论', 'F0', '经济学基础理论', 1
FROM book_category WHERE category_code = 'F' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO book_category (parent_id, category_name, category_code, description, sort_order)
SELECT id, '企业管理', 'F27', '企业经营与管理', 2
FROM book_category WHERE category_code = 'F' AND deleted_at IS NULL
ON CONFLICT (category_code) DO NOTHING;

-- =========================================
-- 2. Map Existing Books to Categories
-- =========================================

-- Map technical books to categories
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'TP312JA%'
    AND c.category_code = 'TP312JA'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'TP312SP%'
    AND c.category_code = 'TP312JA'  -- Spring Boot goes under Java
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map literature books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'I2%'
    AND c.category_code = 'I2'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'I7%'
    AND c.category_code = 'I7'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map history books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'K%'
    AND c.category_code = 'K'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map physics/science books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'P%'
    AND c.category_code = 'P'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map management books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'F27%'
    AND c.category_code = 'F27'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map philosophy books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'B%'
    AND c.category_code = 'B'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map psychology books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'B84%'
    AND c.category_code = 'B'  -- Psychology under Philosophy
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map art books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'J%'
    AND c.category_code = 'J'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- Map education books
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
SELECT b.id, c.id, TRUE
FROM books b
CROSS JOIN book_category c
WHERE b.classification_code LIKE 'G%'
    AND c.category_code = 'G'
    AND b.deleted_at IS NULL
    AND c.deleted_at IS NULL
ON CONFLICT (book_id, category_id) DO NOTHING;

-- =========================================
-- 3. Add Sample File Attachments for Popular Books
-- =========================================

-- Add sample cover images and PDFs for a few books
INSERT INTO book_attachments (
    book_id,
    file_type,
    file_name,
    file_size,
    file_url,
    mime_type,
    checksum,
    description,
    upload_user_name
)
SELECT
    id,
    'COVER',
    'cover_' || isbn || '.jpg',
    FLOOR(RANDOM() * 500000 + 100000)::BIGINT,  -- Random size 100KB-600KB
    'https://minio.library.gcrf.com/book-covers/' || isbn || '.jpg',
    'image/jpeg',
    MD5(RANDOM()::TEXT)::VARCHAR,
    '图书封面',
    'System Admin'
FROM books
WHERE isbn IN ('9787115544063', '9787121411748', '9787302570646')
    AND deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Add sample PDF previews
INSERT INTO book_attachments (
    book_id,
    file_type,
    file_name,
    file_size,
    file_url,
    mime_type,
    checksum,
    description,
    upload_user_name
)
SELECT
    id,
    'PREVIEW',
    'preview_' || isbn || '.pdf',
    FLOOR(RANDOM() * 2000000 + 500000)::BIGINT,  -- Random size 500KB-2.5MB
    'https://minio.library.gcrf.com/book-previews/' || isbn || '.pdf',
    'application/pdf',
    MD5(RANDOM()::TEXT || '2')::VARCHAR,
    '图书预览（前30页）',
    'System Admin'
FROM books
WHERE isbn IN ('9787115544063', '9787121411748')
    AND deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- =========================================
-- 4. Initialize Inventory Logs for Existing Books
-- =========================================

-- Create initial stock entries if not exists
INSERT INTO book_inventory_log (
    book_id,
    transaction_type,
    quantity_change,
    total_before,
    total_after,
    available_before,
    available_after,
    borrowed_before,
    borrowed_after,
    reserved_before,
    reserved_after,
    reason,
    operator_name
)
SELECT
    id,
    'INITIAL_STOCK',
    total_copies,
    0,
    total_copies,
    0,
    available_copies,
    0,
    borrowed_copies,
    0,
    reserved_copies,
    'Initial inventory setup',
    'System Admin'
FROM books
WHERE deleted_at IS NULL
    AND NOT EXISTS (
        SELECT 1 FROM book_inventory_log
        WHERE book_id = books.id AND transaction_type = 'INITIAL_STOCK'
    );

-- =========================================
-- 5. Create Sample Inventory Transactions
-- =========================================

-- Simulate some borrows
INSERT INTO book_inventory_log (
    book_id,
    transaction_type,
    quantity_change,
    total_before,
    total_after,
    available_before,
    available_after,
    borrowed_before,
    borrowed_after,
    reserved_before,
    reserved_after,
    reason,
    operator_id,
    operator_name,
    created_at
)
SELECT
    b.id,
    'BORROW',
    0,
    b.total_copies,
    b.total_copies,
    b.available_copies + 1,
    b.available_copies,
    b.borrowed_copies - 1,
    b.borrowed_copies,
    b.reserved_copies,
    b.reserved_copies,
    'Sample borrow transaction',
    1,
    'Test Librarian',
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (FLOOR(RANDOM() * 30) + 1)
FROM books b
WHERE b.borrowed_copies > 0
    AND b.deleted_at IS NULL
LIMIT 10;

-- Simulate some returns
INSERT INTO book_inventory_log (
    book_id,
    transaction_type,
    quantity_change,
    total_before,
    total_after,
    available_before,
    available_after,
    borrowed_before,
    borrowed_after,
    reserved_before,
    reserved_after,
    reason,
    operator_id,
    operator_name,
    created_at
)
SELECT
    b.id,
    'RETURN',
    0,
    b.total_copies,
    b.total_copies,
    b.available_copies - 1,
    b.available_copies,
    b.borrowed_copies + 1,
    b.borrowed_copies,
    b.reserved_copies,
    b.reserved_copies,
    'Sample return transaction',
    1,
    'Test Librarian',
    CURRENT_TIMESTAMP - INTERVAL '1 hour' * (FLOOR(RANDOM() * 24) + 1)
FROM books b
WHERE b.borrowed_copies > 0
    AND b.deleted_at IS NULL
LIMIT 5;

-- =========================================
-- 6. Create Initial Inventory Snapshot
-- =========================================

-- Create today's snapshot
SELECT create_daily_inventory_snapshot(CURRENT_DATE);

-- Create snapshots for the past 7 days
DO $$
DECLARE
    v_date DATE;
BEGIN
    FOR i IN 1..7 LOOP
        v_date := CURRENT_DATE - i;
        PERFORM create_daily_inventory_snapshot(v_date);
    END LOOP;
END $$;

-- =========================================
-- 7. Update Search Vectors for All Books
-- =========================================

-- Ensure all books have search vectors populated
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
-- 8. Refresh Materialized Views
-- =========================================

-- Refresh the book search materialized view
REFRESH MATERIALIZED VIEW mv_book_search;

-- =========================================
-- 9. Add Sample Search Logs
-- =========================================

INSERT INTO book_search_log (
    search_query,
    search_type,
    result_count,
    execution_time_ms,
    user_id,
    created_at
)
VALUES
    ('Java', 'fulltext', 3, 45, 1, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    ('Spring Boot', 'fuzzy', 1, 62, 2, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    ('红楼梦', 'exact', 1, 23, 3, CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    ('余华', 'fulltext', 1, 34, 1, CURRENT_TIMESTAMP - INTERVAL '4 hours'),
    ('编程', 'smart', 5, 78, 4, CURRENT_TIMESTAMP - INTERVAL '5 hours'),
    ('历史', 'fulltext', 2, 41, 5, CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    ('心理学', 'fuzzy', 1, 55, 2, CURRENT_TIMESTAMP - INTERVAL '7 hours'),
    ('9787115544063', 'exact', 1, 12, 3, CURRENT_TIMESTAMP - INTERVAL '8 hours')
ON CONFLICT DO NOTHING;

-- =========================================
-- 10. Data Statistics Summary
-- =========================================

-- Display seed data statistics
DO $$
DECLARE
    v_category_count INTEGER;
    v_book_count INTEGER;
    v_mapping_count INTEGER;
    v_attachment_count INTEGER;
    v_log_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_category_count FROM book_category WHERE deleted_at IS NULL;
    SELECT COUNT(*) INTO v_book_count FROM books WHERE deleted_at IS NULL;
    SELECT COUNT(*) INTO v_mapping_count FROM book_category_mapping;
    SELECT COUNT(*) INTO v_attachment_count FROM book_attachments WHERE deleted_at IS NULL;
    SELECT COUNT(*) INTO v_log_count FROM book_inventory_log;

    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Seed Data Summary:';
    RAISE NOTICE '-------------------------------------------';
    RAISE NOTICE 'Categories created: %', v_category_count;
    RAISE NOTICE 'Books in system: %', v_book_count;
    RAISE NOTICE 'Book-Category mappings: %', v_mapping_count;
    RAISE NOTICE 'File attachments: %', v_attachment_count;
    RAISE NOTICE 'Inventory log entries: %', v_log_count;
    RAISE NOTICE '===========================================';
END $$;

-- =========================================
-- Migration completed successfully
-- Seed Data:
-- - 30+ categories in hierarchical structure
-- - Book-category mappings for existing books
-- - Sample file attachments
-- - Inventory transaction logs
-- - Search logs for analytics
-- - Updated search vectors
-- - Refreshed materialized views
-- =========================================