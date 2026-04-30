-- =========================================
-- V003: Phase 1 feature tables for system-service
-- Tables: system_config, system_feedback, system_message, system_backup
-- =========================================

CREATE TABLE IF NOT EXISTS system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description  VARCHAR(500),
    config_type  VARCHAR(20) DEFAULT 'STRING',
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT
);

CREATE TABLE IF NOT EXISTS system_feedback (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    user_name    VARCHAR(100),
    title        VARCHAR(200),
    content      TEXT,
    feedback_type VARCHAR(50) DEFAULT 'OTHER',
    status       VARCHAR(20) DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at   TIMESTAMP,
    handled_by   BIGINT,
    response     TEXT
);

CREATE TABLE IF NOT EXISTS system_message (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    title        VARCHAR(200),
    content      TEXT,
    type         VARCHAR(50) DEFAULT 'SYSTEM',
    is_read      BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_user ON system_message(user_id);
CREATE INDEX IF NOT EXISTS idx_message_unread ON system_message(user_id, is_read);

CREATE TABLE IF NOT EXISTS system_backup (
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(255),
    file_size    BIGINT,
    file_path    VARCHAR(500),
    backup_type  VARCHAR(20) DEFAULT 'FULL',
    status       VARCHAR(20) DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Seed default system config
INSERT INTO system_config (config_key, config_value, description) VALUES
('initialized', 'false', '系统是否已完成初始化'),
('library_name', '国创睿峰图书馆', '图书馆名称'),
('library_address', '', '图书馆地址'),
('student_max_borrow', '10', '学生最大借阅册数'),
('teacher_max_borrow', '20', '教师最大借阅册数'),
('borrow_days', '30', '借阅天数'),
('fine_per_day', '0.5', '逾期罚金（元/天）')
ON CONFLICT DO NOTHING;
