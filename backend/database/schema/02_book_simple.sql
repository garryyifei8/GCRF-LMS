-- 图书服务数据库表结构(简化版)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(13) UNIQUE,
    title VARCHAR(500) NOT NULL,
    subtitle VARCHAR(500),
    author VARCHAR(200),
    translator VARCHAR(200),
    publisher VARCHAR(200),
    publish_date DATE,
    edition VARCHAR(50),
    pages INT,
    price DECIMAL(10,2),
    binding VARCHAR(50),
    language VARCHAR(50) DEFAULT 'zh-CN',
    classification_code VARCHAR(50),
    subject_keywords TEXT,
    abstract TEXT,
    cover_url VARCHAR(500),
    total_quantity INT NOT NULL DEFAULT 0,
    available_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS book_items (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    barcode VARCHAR(50) UNIQUE NOT NULL,
    call_number VARCHAR(100),
    location VARCHAR(100),
    shelf_code VARCHAR(50),
    acquisition_date DATE,
    acquisition_price DECIMAL(10,2),
    source VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(50) UNIQUE NOT NULL,
    category_name VARCHAR(200) NOT NULL,
    parent_id BIGINT,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books USING gin(title gin_trgm_ops);
CREATE INDEX idx_books_author ON books USING gin(author gin_trgm_ops);
CREATE INDEX idx_book_items_book_id ON book_items(book_id);
CREATE INDEX idx_book_items_barcode ON book_items(barcode);

INSERT INTO categories (category_code, category_name, level, sort_order) VALUES
('A', '马克思主义、列宁主义、毛泽东思想', 1, 1),
('B', '哲学、宗教', 1, 2),
('T', '工业技术', 1, 18)
ON CONFLICT (category_code) DO NOTHING;
