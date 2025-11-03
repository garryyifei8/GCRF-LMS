-- =========================================
-- 读者服务数据库表结构
-- Database: reader_service
-- Description: 读者管理相关表
-- =========================================

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 读者表 (与Entity匹配)
CREATE TABLE IF NOT EXISTS readers (
    id BIGSERIAL PRIMARY KEY,
    reader_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    id_card VARCHAR(18) UNIQUE,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(500),
    reader_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    student_or_employee_id VARCHAR(50),
    department VARCHAR(200),
    major_or_position VARCHAR(200),
    enrollment_or_employment_year INT,
    card_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    card_issue_date DATE,
    card_expiry_date DATE,
    deposit_amount INT DEFAULT 0,
    deposit_status VARCHAR(20) DEFAULT 'UNPAID',
    credit_score INT DEFAULT 100,
    current_borrow_count INT DEFAULT 0,
    max_borrow_count INT DEFAULT 10,
    total_borrow_count INT DEFAULT 0,
    avatar_url VARCHAR(500),
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted INT DEFAULT 0,
    version INT DEFAULT 0
);

-- 2. 借书卡办理记录表
CREATE TABLE IF NOT EXISTS card_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL REFERENCES readers(id) ON DELETE CASCADE,
    record_type VARCHAR(20) NOT NULL,
    old_card_number VARCHAR(50),
    new_card_number VARCHAR(50),
    fee_amount INT DEFAULT 0,
    payment_status VARCHAR(20) DEFAULT 'PAID',
    payment_method VARCHAR(20),
    reason TEXT,
    operator_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted INT DEFAULT 0,
    version INT DEFAULT 0
);

-- 3. 读者行为日志表
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

-- 4. 读者收藏表
CREATE TABLE IF NOT EXISTS reader_favorites (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    folder_name VARCHAR(100) DEFAULT '默认收藏夹',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(reader_id, book_id)
);

-- 5. 读者评价表
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

-- 6. 读者通知表
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

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_readers_reader_id ON readers(reader_id);
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
