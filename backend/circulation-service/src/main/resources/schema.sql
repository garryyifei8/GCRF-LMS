-- Auto-executed by Spring Boot on startup
DROP TABLE IF EXISTS circulation_records CASCADE;

CREATE TABLE circulation_records (
    id BIGINT PRIMARY KEY,
    book_id BIGINT,
    book_title VARCHAR(255),
    reader_id BIGINT,
    reader_name VARCHAR(100),
    borrow_time TIMESTAMP,
    due_time TIMESTAMP,
    return_time TIMESTAMP,
    renew_count INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    fine_amount BIGINT DEFAULT 0,
    remark TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_circulation_records_book_id ON circulation_records(book_id);
CREATE INDEX IF NOT EXISTS idx_circulation_records_reader_id ON circulation_records(reader_id);
CREATE INDEX IF NOT EXISTS idx_circulation_records_status ON circulation_records(status);
