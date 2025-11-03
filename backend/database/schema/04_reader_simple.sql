-- 读者服务数据库表结构(简化版)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS readers (
    id BIGSERIAL PRIMARY KEY,
    reader_id VARCHAR(50) UNIQUE NOT NULL,
    card_number VARCHAR(50) UNIQUE,
    reader_name VARCHAR(100) NOT NULL,
    reader_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    gender VARCHAR(10),
    id_card VARCHAR(18) UNIQUE,
    phone VARCHAR(20),
    email VARCHAR(100),
    department VARCHAR(200),
    major VARCHAR(200),
    grade VARCHAR(20),
    class_name VARCHAR(50),
    student_type VARCHAR(20),
    photo_url VARCHAR(500),
    face_features TEXT,
    deposit_amount DECIMAL(10,2) DEFAULT 0,
    credit_score INT DEFAULT 100,
    max_borrow_quantity INT DEFAULT 10,
    current_borrow_count INT DEFAULT 0,
    total_borrow_count INT DEFAULT 0,
    overdue_count INT DEFAULT 0,
    card_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    account_balance DECIMAL(10,2) DEFAULT 0,
    issue_date DATE,
    expire_date DATE,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reader_favorites (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    folder_name VARCHAR(100) DEFAULT '默认收藏夹',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(reader_id, book_id)
);

CREATE INDEX idx_readers_reader_id ON readers(reader_id);
CREATE INDEX idx_readers_phone ON readers(phone);
CREATE INDEX idx_readers_card_status ON readers(card_status);
