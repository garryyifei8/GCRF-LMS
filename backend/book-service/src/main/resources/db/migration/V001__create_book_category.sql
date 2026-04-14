-- =========================================
-- V001: Book Category System with Hierarchical Structure
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "ltree";  -- For hierarchical path operations

-- =========================================
-- 1. Book Category Table (Hierarchical up to 5 levels)
-- =========================================
CREATE TABLE IF NOT EXISTS book_category (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Hierarchical structure
    parent_id BIGINT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL UNIQUE,

    -- Materialized path for fast tree queries
    -- Format: 001.002.003 (each level is 3 digits)
    path VARCHAR(255) NOT NULL,
    level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1 AND level <= 5),

    -- Category metadata
    description TEXT,
    icon VARCHAR(100),  -- Icon class name for UI
    color VARCHAR(20),  -- Color code for UI display
    sort_order INTEGER DEFAULT 0,

    -- Statistics (denormalized for performance)
    book_count INTEGER DEFAULT 0,
    child_count INTEGER DEFAULT 0,

    -- Status and audit
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,  -- Soft delete

    -- Constraints
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id)
        REFERENCES book_category(id) ON DELETE RESTRICT,
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_category_level CHECK (level <= 5),
    CONSTRAINT chk_parent_not_self CHECK (parent_id != id)
);

-- =========================================
-- 2. Book-Category Many-to-Many Relationship
-- =========================================
CREATE TABLE IF NOT EXISTS book_category_mapping (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,  -- Primary category for the book
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_mapping_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_category FOREIGN KEY (category_id)
        REFERENCES book_category(id) ON DELETE RESTRICT,

    -- Unique constraint
    CONSTRAINT uk_book_category UNIQUE (book_id, category_id)
);

-- =========================================
-- 3. Create Indexes for Performance
-- =========================================

-- Category table indexes
CREATE INDEX idx_category_parent_id ON book_category(parent_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_category_path ON book_category(path)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_category_path_pattern ON book_category(path varchar_pattern_ops)
    WHERE deleted_at IS NULL;  -- For LIKE queries
CREATE INDEX idx_category_level ON book_category(level)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_category_status ON book_category(status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_category_sort_order ON book_category(parent_id, sort_order)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_category_code ON book_category(category_code)
    WHERE deleted_at IS NULL;

-- Mapping table indexes
CREATE INDEX idx_mapping_book_id ON book_category_mapping(book_id);
CREATE INDEX idx_mapping_category_id ON book_category_mapping(category_id);
CREATE INDEX idx_mapping_primary ON book_category_mapping(book_id, is_primary)
    WHERE is_primary = TRUE;

-- =========================================
-- 4. Functions for Tree Operations
-- =========================================

-- Function to generate path for a new category
CREATE OR REPLACE FUNCTION generate_category_path(p_parent_id BIGINT)
RETURNS VARCHAR AS $$
DECLARE
    v_parent_path VARCHAR;
    v_next_number INTEGER;
    v_new_path VARCHAR;
BEGIN
    IF p_parent_id IS NULL THEN
        -- Root level category
        SELECT COALESCE(MAX(SUBSTRING(path FROM '^(\d{3})')::INTEGER), 0) + 1
        INTO v_next_number
        FROM book_category
        WHERE parent_id IS NULL AND deleted_at IS NULL;

        v_new_path := LPAD(v_next_number::TEXT, 3, '0');
    ELSE
        -- Sub-category
        SELECT path INTO v_parent_path
        FROM book_category
        WHERE id = p_parent_id AND deleted_at IS NULL;

        IF v_parent_path IS NULL THEN
            RAISE EXCEPTION 'Parent category not found: %', p_parent_id;
        END IF;

        -- Check parent level
        IF LENGTH(v_parent_path) >= 15 THEN  -- 5 levels * 3 digits + 4 dots = 19 chars max
            RAISE EXCEPTION 'Maximum category depth (5 levels) exceeded';
        END IF;

        -- Get next number for this parent
        SELECT COALESCE(MAX(
            SUBSTRING(path FROM LENGTH(v_parent_path) + 2 FOR 3)::INTEGER
        ), 0) + 1
        INTO v_next_number
        FROM book_category
        WHERE parent_id = p_parent_id AND deleted_at IS NULL;

        v_new_path := v_parent_path || '.' || LPAD(v_next_number::TEXT, 3, '0');
    END IF;

    RETURN v_new_path;
END;
$$ LANGUAGE plpgsql;

-- Function to get all descendant categories
CREATE OR REPLACE FUNCTION get_category_descendants(p_category_id BIGINT)
RETURNS TABLE(
    id BIGINT,
    parent_id BIGINT,
    category_name VARCHAR,
    category_code VARCHAR,
    path VARCHAR,
    level INTEGER,
    book_count INTEGER
) AS $$
DECLARE
    v_path VARCHAR;
BEGIN
    SELECT c.path INTO v_path
    FROM book_category c
    WHERE c.id = p_category_id AND c.deleted_at IS NULL;

    IF v_path IS NULL THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT
        c.id,
        c.parent_id,
        c.category_name,
        c.category_code,
        c.path,
        c.level,
        c.book_count
    FROM book_category c
    WHERE c.path LIKE v_path || '.%'
        AND c.deleted_at IS NULL
    ORDER BY c.path;
END;
$$ LANGUAGE plpgsql;

-- Function to get all ancestor categories
CREATE OR REPLACE FUNCTION get_category_ancestors(p_category_id BIGINT)
RETURNS TABLE(
    id BIGINT,
    parent_id BIGINT,
    category_name VARCHAR,
    category_code VARCHAR,
    path VARCHAR,
    level INTEGER
) AS $$
DECLARE
    v_path VARCHAR;
    v_paths TEXT[];
    v_i INTEGER;
BEGIN
    SELECT c.path INTO v_path
    FROM book_category c
    WHERE c.id = p_category_id AND c.deleted_at IS NULL;

    IF v_path IS NULL THEN
        RETURN;
    END IF;

    -- Generate all ancestor paths
    v_paths := ARRAY[]::TEXT[];
    WHILE v_path LIKE '%.%' LOOP
        v_path := SUBSTRING(v_path FROM 1 FOR LENGTH(v_path) - 4);
        v_paths := array_append(v_paths, v_path);
    END LOOP;

    -- Include root if not already included
    IF LENGTH(v_path) = 3 THEN
        v_paths := array_append(v_paths, v_path);
    END IF;

    RETURN QUERY
    SELECT
        c.id,
        c.parent_id,
        c.category_name,
        c.category_code,
        c.path,
        c.level
    FROM book_category c
    WHERE c.path = ANY(v_paths)
        AND c.deleted_at IS NULL
    ORDER BY c.level;
END;
$$ LANGUAGE plpgsql;

-- Function to update book counts in category tree
CREATE OR REPLACE FUNCTION update_category_book_count(p_category_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_count INTEGER;
    v_ancestor_id BIGINT;
BEGIN
    -- Count books directly in this category
    SELECT COUNT(DISTINCT bcm.book_id)
    INTO v_count
    FROM book_category_mapping bcm
    JOIN books b ON bcm.book_id = b.id
    WHERE bcm.category_id = p_category_id
        AND b.deleted_at IS NULL;

    -- Update this category's count
    UPDATE book_category
    SET book_count = v_count,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_category_id;

    -- Update all ancestor categories
    FOR v_ancestor_id IN
        SELECT id FROM get_category_ancestors(p_category_id)
    LOOP
        SELECT COUNT(DISTINCT bcm.book_id)
        INTO v_count
        FROM book_category_mapping bcm
        JOIN books b ON bcm.book_id = b.id
        JOIN book_category c ON bcm.category_id = c.id
        WHERE (c.id = v_ancestor_id OR c.path LIKE
            (SELECT path FROM book_category WHERE id = v_ancestor_id) || '.%')
            AND b.deleted_at IS NULL
            AND c.deleted_at IS NULL;

        UPDATE book_category
        SET book_count = v_count,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = v_ancestor_id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 5. Triggers
-- =========================================

-- Trigger to auto-generate path and level on insert
CREATE OR REPLACE FUNCTION before_category_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Generate path
    NEW.path := generate_category_path(NEW.parent_id);

    -- Calculate level
    NEW.level := (LENGTH(NEW.path) - LENGTH(REPLACE(NEW.path, '.', ''))) + 1;

    -- Update parent's child count
    IF NEW.parent_id IS NOT NULL THEN
        UPDATE book_category
        SET child_count = child_count + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.parent_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_category_insert
    BEFORE INSERT ON book_category
    FOR EACH ROW
    EXECUTE FUNCTION before_category_insert();

-- Trigger to update timestamps
CREATE OR REPLACE FUNCTION update_category_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_category_timestamp
    BEFORE UPDATE ON book_category
    FOR EACH ROW
    EXECUTE FUNCTION update_category_timestamp();

-- Trigger to handle category soft delete
CREATE OR REPLACE FUNCTION before_category_soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        -- Check for child categories
        IF EXISTS (
            SELECT 1 FROM book_category
            WHERE parent_id = NEW.id AND deleted_at IS NULL
        ) THEN
            RAISE EXCEPTION 'Cannot delete category with active child categories';
        END IF;

        -- Update parent's child count
        IF NEW.parent_id IS NOT NULL THEN
            UPDATE book_category
            SET child_count = child_count - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = NEW.parent_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_category_soft_delete
    BEFORE UPDATE OF deleted_at ON book_category
    FOR EACH ROW
    EXECUTE FUNCTION before_category_soft_delete();

-- Trigger to update category book count on mapping change
CREATE OR REPLACE FUNCTION after_book_category_mapping_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM update_category_book_count(NEW.category_id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM update_category_book_count(OLD.category_id);
    ELSIF TG_OP = 'UPDATE' AND NEW.category_id != OLD.category_id THEN
        PERFORM update_category_book_count(OLD.category_id);
        PERFORM update_category_book_count(NEW.category_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_book_category_mapping_change
    AFTER INSERT OR UPDATE OR DELETE ON book_category_mapping
    FOR EACH ROW
    EXECUTE FUNCTION after_book_category_mapping_change();

-- =========================================
-- 6. Views for Common Queries
-- =========================================

-- View for category tree with full paths
CREATE OR REPLACE VIEW v_category_tree AS
WITH RECURSIVE category_tree AS (
    -- Root categories
    SELECT
        id,
        parent_id,
        category_name,
        category_code,
        path,
        level,
        category_name AS full_name,
        book_count,
        child_count,
        sort_order,
        status
    FROM book_category
    WHERE parent_id IS NULL AND deleted_at IS NULL

    UNION ALL

    -- Child categories
    SELECT
        c.id,
        c.parent_id,
        c.category_name,
        c.category_code,
        c.path,
        c.level,
        ct.full_name || ' > ' || c.category_name AS full_name,
        c.book_count,
        c.child_count,
        c.sort_order,
        c.status
    FROM book_category c
    INNER JOIN category_tree ct ON c.parent_id = ct.id
    WHERE c.deleted_at IS NULL
)
SELECT * FROM category_tree
ORDER BY path;

-- =========================================
-- 7. Comments
-- =========================================
COMMENT ON TABLE book_category IS '图书分类表，支持最多5级层级结构';
COMMENT ON COLUMN book_category.id IS '分类ID';
COMMENT ON COLUMN book_category.parent_id IS '父分类ID，NULL表示顶级分类';
COMMENT ON COLUMN book_category.category_name IS '分类名称';
COMMENT ON COLUMN book_category.category_code IS '分类代码，全局唯一';
COMMENT ON COLUMN book_category.path IS '物化路径，格式：001.002.003，用于快速查询';
COMMENT ON COLUMN book_category.level IS '层级深度，1-5';
COMMENT ON COLUMN book_category.description IS '分类描述';
COMMENT ON COLUMN book_category.icon IS '分类图标';
COMMENT ON COLUMN book_category.color IS '分类颜色';
COMMENT ON COLUMN book_category.sort_order IS '排序顺序';
COMMENT ON COLUMN book_category.book_count IS '该分类及子分类下的图书总数（冗余字段，提升性能）';
COMMENT ON COLUMN book_category.child_count IS '直接子分类数量（冗余字段，提升性能）';
COMMENT ON COLUMN book_category.status IS '状态：ACTIVE-启用，INACTIVE-禁用';

COMMENT ON TABLE book_category_mapping IS '图书-分类映射表（多对多关系）';
COMMENT ON COLUMN book_category_mapping.book_id IS '图书ID';
COMMENT ON COLUMN book_category_mapping.category_id IS '分类ID';
COMMENT ON COLUMN book_category_mapping.is_primary IS '是否为主分类';

COMMENT ON FUNCTION generate_category_path IS '生成分类的物化路径';
COMMENT ON FUNCTION get_category_descendants IS '获取指定分类的所有子孙分类';
COMMENT ON FUNCTION get_category_ancestors IS '获取指定分类的所有祖先分类';
COMMENT ON FUNCTION update_category_book_count IS '更新分类树中的图书数量统计';

-- =========================================
-- Migration completed successfully
-- Tables: 2 (book_category, book_category_mapping)
-- Indexes: 9
-- Functions: 4
-- Triggers: 4
-- Views: 1
-- =========================================