-- =========================================
-- V001: Baseline migration for reader-service
-- Database: reader_service
-- Description: Creates readers, reader_types and related tables
-- =========================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Readers table
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

-- 2. Card records table
CREATE TABLE IF NOT EXISTS card_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL REFERENCES readers(id) ON DELETE CASCADE,
    record_type VARCHAR(20) NOT NULL,
    old_card_number VARCHAR(50),
    new_card_number VARCHAR(50),
    fee_amount DECIMAL(10,2) DEFAULT 0,
    payment_status VARCHAR(20) DEFAULT 'PAID',
    payment_method VARCHAR(20),
    reason TEXT,
    operator_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Reader behavior logs table
CREATE TABLE IF NOT EXISTS reader_behavior_logs (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    behavior_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    behavior_detail JSONB,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Reader favorites table
CREATE TABLE IF NOT EXISTS reader_favorites (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    folder_name VARCHAR(100) DEFAULT '默认收藏夹',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(reader_id, book_id)
);

-- 5. Reader reviews table
CREATE TABLE IF NOT EXISTS reader_reviews (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    rating INT NOT NULL,
    review_title VARCHAR(200),
    review_content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    helpful_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Reader notifications table
CREATE TABLE IF NOT EXISTS reader_notifications (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    send_channel VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    read_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Reader types table
CREATE TABLE IF NOT EXISTS reader_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    max_borrow_count INTEGER NOT NULL DEFAULT 5,
    max_borrow_days INTEGER NOT NULL DEFAULT 30,
    max_renew_count INTEGER NOT NULL DEFAULT 1,
    deposit_amount INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_readers_reader_id ON readers(reader_id);
CREATE INDEX IF NOT EXISTS idx_readers_card_number ON readers(card_number);
CREATE INDEX IF NOT EXISTS idx_readers_phone ON readers(phone);
CREATE INDEX IF NOT EXISTS idx_readers_email ON readers(email);
CREATE INDEX IF NOT EXISTS idx_readers_reader_type ON readers(reader_type);
CREATE INDEX IF NOT EXISTS idx_readers_card_status ON readers(card_status);
CREATE INDEX IF NOT EXISTS idx_card_records_reader_id ON card_records(reader_id);
CREATE INDEX IF NOT EXISTS idx_reader_behavior_logs_reader_id ON reader_behavior_logs(reader_id);
CREATE INDEX IF NOT EXISTS idx_reader_behavior_logs_created_at ON reader_behavior_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_reader_favorites_reader_id ON reader_favorites(reader_id);
CREATE INDEX IF NOT EXISTS idx_reader_favorites_book_id ON reader_favorites(book_id);
CREATE INDEX IF NOT EXISTS idx_reader_reviews_reader_id ON reader_reviews(reader_id);
CREATE INDEX IF NOT EXISTS idx_reader_reviews_book_id ON reader_reviews(book_id);
CREATE INDEX IF NOT EXISTS idx_reader_notifications_reader_id ON reader_notifications(reader_id);
CREATE INDEX IF NOT EXISTS idx_reader_notifications_is_read ON reader_notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_reader_types_type_code ON reader_types(type_code);
CREATE INDEX IF NOT EXISTS idx_reader_types_status ON reader_types(status);

-- Default reader types
INSERT INTO reader_types (type_code, type_name, max_borrow_count, max_borrow_days, max_renew_count, sort_order) VALUES
    ('STUDENT', '学生', 10, 30, 2, 1),
    ('TEACHER', '教师', 20, 60, 3, 2),
    ('STAFF', '教职工', 15, 45, 2, 3),
    ('EXTERNAL', '校外读者', 3, 15, 1, 4)
ON CONFLICT (type_code) DO NOTHING;
