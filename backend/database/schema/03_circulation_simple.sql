-- 流通服务数据库表结构(简化版)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS circulation_records (
    id BIGSERIAL PRIMARY KEY,
    record_code VARCHAR(50) UNIQUE NOT NULL,
    book_item_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    renew_count INT DEFAULT 0,
    renew_limit INT DEFAULT 3,
    overdue_days INT DEFAULT 0,
    fine_amount DECIMAL(10,2) DEFAULT 0,
    fine_paid BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    operator_id BIGINT,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservation_records (
    id BIGSERIAL PRIMARY KEY,
    reservation_code VARCHAR(50) UNIQUE NOT NULL,
    book_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_date TIMESTAMP NOT NULL,
    notification_sent BOOLEAN DEFAULT FALSE,
    notification_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancel_reason VARCHAR(200),
    picked_up_time TIMESTAMP,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fine_records (
    id BIGSERIAL PRIMARY KEY,
    fine_code VARCHAR(50) UNIQUE NOT NULL,
    circulation_record_id BIGINT REFERENCES circulation_records(id) ON DELETE SET NULL,
    reader_id BIGINT NOT NULL,
    fine_type VARCHAR(20) NOT NULL,
    fine_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) DEFAULT 0,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_method VARCHAR(20),
    payment_time TIMESTAMP,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_circulation_records_book_id ON circulation_records(book_id);
CREATE INDEX idx_circulation_records_reader_id ON circulation_records(reader_id);
CREATE INDEX idx_circulation_records_status ON circulation_records(status);
