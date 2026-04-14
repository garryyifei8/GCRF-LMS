-- =========================================
-- V000: Create Books Table (Base Table)
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-12-20
-- Description: Creates the base books table that other migrations depend on
-- =========================================

CREATE TABLE IF NOT EXISTS books (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Core book information
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(255),
    publisher VARCHAR(255),
    publish_date DATE,
    pages INTEGER,
    language VARCHAR(50) DEFAULT 'Chinese',

    -- Physical properties
    price DECIMAL(10, 2),
    cover_url TEXT,
    description TEXT,

    -- Inventory
    total_quantity INTEGER DEFAULT 1,
    available_quantity INTEGER DEFAULT 1,

    -- Status and classification
    status VARCHAR(20) DEFAULT 'ACTIVE',
    category_id BIGINT,
    category_code VARCHAR(50),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_book_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DAMAGED', 'LOST')),
    CONSTRAINT chk_book_quantity CHECK (total_quantity >= 0 AND available_quantity >= 0)
);

-- =========================================
-- Indexes for Performance
-- =========================================
CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_books_author ON books(author) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_books_status ON books(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_books_created_at ON books(created_at);

-- =========================================
-- Trigger for updated_at
-- =========================================
CREATE OR REPLACE FUNCTION update_books_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_books_timestamp
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION update_books_timestamp();

-- =========================================
-- Comments
-- =========================================
COMMENT ON TABLE books IS '图书基本信息表';
COMMENT ON COLUMN books.id IS '图书ID';
COMMENT ON COLUMN books.isbn IS 'ISBN号';
COMMENT ON COLUMN books.title IS '书名';
COMMENT ON COLUMN books.author IS '作者';
COMMENT ON COLUMN books.publisher IS '出版社';
COMMENT ON COLUMN books.publish_date IS '出版日期';
COMMENT ON COLUMN books.pages IS '页数';
COMMENT ON COLUMN books.language IS '语言';
COMMENT ON COLUMN books.price IS '价格';
COMMENT ON COLUMN books.cover_url IS '封面URL';
COMMENT ON COLUMN books.description IS '简介';
COMMENT ON COLUMN books.total_quantity IS '馆藏总数';
COMMENT ON COLUMN books.available_quantity IS '可借数量';
COMMENT ON COLUMN books.status IS '状态：ACTIVE-正常, INACTIVE-下架, DAMAGED-损坏, LOST-遗失';
COMMENT ON COLUMN books.category_id IS '分类ID';
COMMENT ON COLUMN books.category_code IS '分类代码';
