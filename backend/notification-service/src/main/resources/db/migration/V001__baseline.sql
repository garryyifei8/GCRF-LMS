-- =========================================
-- V001: Baseline migration for notification-service
-- Database: notification_service
-- Description: Creates notification, email/sms log, and template tables
-- NOTE: Partitioning for email_logs/sms_logs can be added in future V002
-- =========================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Notifications table (站内信)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    extra_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_notification_type CHECK (notification_type IN ('SYSTEM', 'USER', 'ACTIVITY', 'ANNOUNCEMENT')),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

-- 2. Email logs table (邮件发送记录) - NON-partitioned for baseline
CREATE TABLE IF NOT EXISTS email_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    template_id BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_email_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
);

-- 3. SMS logs table (短信发送记录) - NON-partitioned for baseline
CREATE TABLE IF NOT EXISTS sms_logs (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sms_type VARCHAR(50) NOT NULL,
    verification_code VARCHAR(10),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sms_type CHECK (sms_type IN ('VERIFICATION', 'NOTIFICATION', 'MARKETING')),
    CONSTRAINT chk_sms_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
);

-- 4. Notification templates table (通知模板)
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    content TEXT NOT NULL,
    variables JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_template_type CHECK (template_type IN ('EMAIL', 'SMS', 'NOTIFICATION')),
    CONSTRAINT chk_template_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 5. Notification subscriptions table (用户订阅配置)
CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_types JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================
-- Indexes
-- =========================================

-- notifications indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

-- email_logs indexes
CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs(recipient);
CREATE INDEX IF NOT EXISTS idx_email_logs_status ON email_logs(status);
CREATE INDEX IF NOT EXISTS idx_email_logs_created_at ON email_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_logs_template_id ON email_logs(template_id);

-- sms_logs indexes
CREATE INDEX IF NOT EXISTS idx_sms_logs_phone ON sms_logs(phone_number);
CREATE INDEX IF NOT EXISTS idx_sms_logs_status ON sms_logs(status);
CREATE INDEX IF NOT EXISTS idx_sms_logs_created_at ON sms_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sms_logs_type ON sms_logs(sms_type);
CREATE INDEX IF NOT EXISTS idx_sms_logs_verification ON sms_logs(phone_number, verification_code) WHERE verification_code IS NOT NULL;

-- notification_templates indexes
CREATE INDEX IF NOT EXISTS idx_templates_code ON notification_templates(template_code);
CREATE INDEX IF NOT EXISTS idx_templates_type ON notification_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_templates_status ON notification_templates(status);

-- notification_subscriptions indexes
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON notification_subscriptions(user_id);

-- =========================================
-- Foreign key constraints
-- =========================================

-- email_logs -> notification_templates
ALTER TABLE email_logs
ADD CONSTRAINT fk_email_logs_template
FOREIGN KEY (template_id)
REFERENCES notification_templates(id)
ON DELETE SET NULL;

-- =========================================
-- Default notification templates
-- =========================================

INSERT INTO notification_templates (template_code, template_name, template_type, subject, content, variables, status) VALUES
-- 1. Welcome message (in-app notification)
('WELCOME', '欢迎消息', 'NOTIFICATION', '欢迎加入国创睿峰智能图书馆',
'尊敬的{{username}},您好!\n\n欢迎使用国创睿峰智能图书馆管理系统。系统已为您开通账号,您可以开始使用图书借阅、预约等服务。\n\n如有任何问题,请联系管理员。',
'["username"]', 'ACTIVE'),

-- 2. Verification code (SMS)
('VERIFICATION_CODE', '验证码短信', 'SMS', NULL,
'【国创睿峰】您的验证码是: {{code}},有效期{{expireTime}}分钟。如非本人操作,请忽略本短信。',
'["code", "expireTime"]', 'ACTIVE'),

-- 3. Borrow reminder (Email)
('BORROW_REMINDER', '借阅到期提醒', 'EMAIL', '图书即将到期提醒',
'尊敬的{{username}}:\n\n您借阅的图书《{{bookTitle}}》将于{{dueDate}}到期,请及时归还或续借。\n\n逾期归还将产生滞纳金,请注意!\n\n国创睿峰智能图书馆\n{{systemDate}}',
'["username", "bookTitle", "dueDate", "systemDate"]', 'ACTIVE'),

-- 4. Reserve success (in-app notification)
('RESERVE_SUCCESS', '预约成功', 'NOTIFICATION', '图书预约成功通知',
'您好,{{username}}!\n\n您预约的图书《{{bookTitle}}》已成功,预约单号: {{reserveNo}}。\n\n图书到馆后我们会通知您取书,请保持手机畅通。预约有效期为{{validDays}}天。',
'["username", "bookTitle", "reserveNo", "validDays"]', 'ACTIVE'),

-- 5. Overdue notice (Email)
('OVERDUE_NOTICE', '图书逾期通知', 'EMAIL', '图书逾期通知 - 请尽快归还',
'{{username}},您好:\n\n您借阅的图书《{{bookTitle}}》已逾期{{overdueDays}}天,产生滞纳金{{fine}}元。\n\n请尽快到馆归还图书并缴纳滞纳金,以免影响您的信用记录。\n\n联系电话: {{contactPhone}}\n\n国创睿峰智能图书馆',
'["username", "bookTitle", "overdueDays", "fine", "contactPhone"]', 'ACTIVE')

ON CONFLICT (template_code) DO NOTHING;
