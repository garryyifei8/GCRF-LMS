-- =========================================
-- 通知服务数据库表结构
-- Database: notification_service
-- Description: 站内信、邮件、短信通知相关表
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建notification_service数据库
-- CREATE DATABASE notification_service;
-- 连接到notification_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 站内信表 (notifications)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- 接收用户ID (关联auth_service.users.id)
    title VARCHAR(200) NOT NULL, -- 标题
    content TEXT NOT NULL, -- 内容
    notification_type VARCHAR(50) NOT NULL, -- 类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急
    is_read BOOLEAN NOT NULL DEFAULT FALSE, -- 是否已读
    read_at TIMESTAMP, -- 已读时间
    extra_data JSONB, -- 扩展数据 (JSON格式,用于存储业务相关的额外信息)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- 软删除标记
    CONSTRAINT chk_notification_type CHECK (notification_type IN ('SYSTEM', 'USER', 'ACTIVITY', 'ANNOUNCEMENT')),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

-- 2. 邮件发送记录表 (email_logs)
CREATE TABLE IF NOT EXISTS email_logs (
    id BIGSERIAL,
    recipient VARCHAR(255) NOT NULL, -- 收件人邮箱
    subject VARCHAR(500) NOT NULL, -- 邮件主题
    content TEXT NOT NULL, -- 邮件内容
    template_id BIGINT, -- 模板ID (关联notification_templates.id, 可选)
    status VARCHAR(20) NOT NULL, -- 状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败
    error_message TEXT, -- 错误信息
    retry_count INT NOT NULL DEFAULT 0, -- 重试次数
    sent_at TIMESTAMP, -- 发送时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at), -- 复合主键,包含分区键
    CONSTRAINT chk_email_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
) PARTITION BY RANGE (created_at);

-- 3. 短信发送记录表 (sms_logs)
CREATE TABLE IF NOT EXISTS sms_logs (
    id BIGSERIAL,
    phone_number VARCHAR(20) NOT NULL, -- 手机号
    content TEXT NOT NULL, -- 短信内容
    sms_type VARCHAR(50) NOT NULL, -- 类型: VERIFICATION-验证码, NOTIFICATION-通知, MARKETING-营销
    verification_code VARCHAR(10), -- 验证码 (如果是验证码短信)
    status VARCHAR(20) NOT NULL, -- 状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败
    error_message TEXT, -- 错误信息
    retry_count INT NOT NULL DEFAULT 0, -- 重试次数
    expires_at TIMESTAMP, -- 过期时间 (用于验证码)
    sent_at TIMESTAMP, -- 发送时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at), -- 复合主键,包含分区键
    CONSTRAINT chk_sms_type CHECK (sms_type IN ('VERIFICATION', 'NOTIFICATION', 'MARKETING')),
    CONSTRAINT chk_sms_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
) PARTITION BY RANGE (created_at);

-- 4. 通知模板表 (notification_templates)
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) UNIQUE NOT NULL, -- 模板编码
    template_name VARCHAR(200) NOT NULL, -- 模板名称
    template_type VARCHAR(50) NOT NULL, -- 类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信
    subject VARCHAR(500), -- 主题 (用于邮件和站内信)
    content TEXT NOT NULL, -- 内容模板 (支持变量 {{variable}})
    variables JSONB, -- 变量定义 (JSON数组, 例如: ["username", "code", "expireTime"])
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE-启用, INACTIVE-停用
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- 软删除标记
    CONSTRAINT chk_template_type CHECK (template_type IN ('EMAIL', 'SMS', 'NOTIFICATION')),
    CONSTRAINT chk_template_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 5. 用户订阅配置表 (notification_subscriptions)
CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- 用户ID (关联auth_service.users.id)
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE, -- 邮件通知开关
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE, -- 短信通知开关
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE, -- 站内信开关
    subscribed_types JSONB, -- 订阅的通知类型 (JSON数组, 例如: ["SYSTEM", "ACTIVITY"])
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================
-- 创建分区表 (按月分区以优化性能)
-- =========================================

-- email_logs 分区 (当前月 + 未来3个月)
CREATE TABLE IF NOT EXISTS email_logs_2025_10 PARTITION OF email_logs
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE IF NOT EXISTS email_logs_2025_11 PARTITION OF email_logs
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE IF NOT EXISTS email_logs_2025_12 PARTITION OF email_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

CREATE TABLE IF NOT EXISTS email_logs_2026_01 PARTITION OF email_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- sms_logs 分区 (当前月 + 未来3个月)
CREATE TABLE IF NOT EXISTS sms_logs_2025_10 PARTITION OF sms_logs
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE IF NOT EXISTS sms_logs_2025_11 PARTITION OF sms_logs
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE IF NOT EXISTS sms_logs_2025_12 PARTITION OF sms_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

CREATE TABLE IF NOT EXISTS sms_logs_2026_01 PARTITION OF sms_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- =========================================
-- 创建索引
-- =========================================

-- notifications 表索引
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

-- email_logs 表索引
CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs(recipient);
CREATE INDEX IF NOT EXISTS idx_email_logs_status ON email_logs(status);
CREATE INDEX IF NOT EXISTS idx_email_logs_created_at ON email_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_logs_template_id ON email_logs(template_id);

-- sms_logs 表索引
CREATE INDEX IF NOT EXISTS idx_sms_logs_phone ON sms_logs(phone_number);
CREATE INDEX IF NOT EXISTS idx_sms_logs_status ON sms_logs(status);
CREATE INDEX IF NOT EXISTS idx_sms_logs_created_at ON sms_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sms_logs_type ON sms_logs(sms_type);
CREATE INDEX IF NOT EXISTS idx_sms_logs_verification ON sms_logs(phone_number, verification_code) WHERE verification_code IS NOT NULL;

-- notification_templates 表索引
CREATE INDEX IF NOT EXISTS idx_templates_code ON notification_templates(template_code);
CREATE INDEX IF NOT EXISTS idx_templates_type ON notification_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_templates_status ON notification_templates(status);

-- notification_subscriptions 表索引
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON notification_subscriptions(user_id);

-- =========================================
-- 添加外键约束
-- =========================================

-- email_logs 模板外键 (可选,允许NULL)
ALTER TABLE email_logs
ADD CONSTRAINT fk_email_logs_template
FOREIGN KEY (template_id)
REFERENCES notification_templates(id)
ON DELETE SET NULL;

-- =========================================
-- 添加表注释
-- =========================================

COMMENT ON TABLE notifications IS '站内信表 - 存储系统发送给用户的各类站内消息';
COMMENT ON TABLE email_logs IS '邮件发送记录表 - 记录所有邮件发送历史,按月分区';
COMMENT ON TABLE sms_logs IS '短信发送记录表 - 记录所有短信发送历史,按月分区';
COMMENT ON TABLE notification_templates IS '通知模板表 - 存储邮件、短信、站内信的消息模板';
COMMENT ON TABLE notification_subscriptions IS '用户订阅配置表 - 存储用户的通知偏好设置';

-- =========================================
-- 添加列注释 - notifications
-- =========================================

COMMENT ON COLUMN notifications.id IS '主键ID';
COMMENT ON COLUMN notifications.user_id IS '接收用户ID (关联auth_service.users.id)';
COMMENT ON COLUMN notifications.title IS '通知标题';
COMMENT ON COLUMN notifications.content IS '通知内容';
COMMENT ON COLUMN notifications.notification_type IS '通知类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告';
COMMENT ON COLUMN notifications.priority IS '优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急';
COMMENT ON COLUMN notifications.is_read IS '是否已读';
COMMENT ON COLUMN notifications.read_at IS '已读时间';
COMMENT ON COLUMN notifications.extra_data IS '扩展数据 (JSONB格式,存储业务相关的额外信息)';
COMMENT ON COLUMN notifications.created_at IS '创建时间';
COMMENT ON COLUMN notifications.deleted_at IS '软删除时间';

-- =========================================
-- 添加列注释 - email_logs
-- =========================================

COMMENT ON COLUMN email_logs.id IS '主键ID';
COMMENT ON COLUMN email_logs.recipient IS '收件人邮箱地址';
COMMENT ON COLUMN email_logs.subject IS '邮件主题';
COMMENT ON COLUMN email_logs.content IS '邮件内容 (HTML或纯文本)';
COMMENT ON COLUMN email_logs.template_id IS '模板ID (关联notification_templates.id, 可选)';
COMMENT ON COLUMN email_logs.status IS '发送状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败';
COMMENT ON COLUMN email_logs.error_message IS '失败时的错误信息';
COMMENT ON COLUMN email_logs.retry_count IS '重试次数';
COMMENT ON COLUMN email_logs.sent_at IS '实际发送时间';
COMMENT ON COLUMN email_logs.created_at IS '创建时间';

-- =========================================
-- 添加列注释 - sms_logs
-- =========================================

COMMENT ON COLUMN sms_logs.id IS '主键ID';
COMMENT ON COLUMN sms_logs.phone_number IS '接收手机号';
COMMENT ON COLUMN sms_logs.content IS '短信内容';
COMMENT ON COLUMN sms_logs.sms_type IS '短信类型: VERIFICATION-验证码, NOTIFICATION-通知, MARKETING-营销';
COMMENT ON COLUMN sms_logs.verification_code IS '验证码 (如果是验证码短信)';
COMMENT ON COLUMN sms_logs.status IS '发送状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败';
COMMENT ON COLUMN sms_logs.error_message IS '失败时的错误信息';
COMMENT ON COLUMN sms_logs.retry_count IS '重试次数';
COMMENT ON COLUMN sms_logs.expires_at IS '验证码过期时间';
COMMENT ON COLUMN sms_logs.sent_at IS '实际发送时间';
COMMENT ON COLUMN sms_logs.created_at IS '创建时间';

-- =========================================
-- 添加列注释 - notification_templates
-- =========================================

COMMENT ON COLUMN notification_templates.id IS '主键ID';
COMMENT ON COLUMN notification_templates.template_code IS '模板唯一编码';
COMMENT ON COLUMN notification_templates.template_name IS '模板名称';
COMMENT ON COLUMN notification_templates.template_type IS '模板类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信';
COMMENT ON COLUMN notification_templates.subject IS '模板主题 (用于邮件和站内信)';
COMMENT ON COLUMN notification_templates.content IS '模板内容 (支持变量占位符 {{variable}})';
COMMENT ON COLUMN notification_templates.variables IS '变量定义 (JSONB数组, 例如: ["username", "code", "expireTime"])';
COMMENT ON COLUMN notification_templates.status IS '状态: ACTIVE-启用, INACTIVE-停用';
COMMENT ON COLUMN notification_templates.created_at IS '创建时间';
COMMENT ON COLUMN notification_templates.updated_at IS '更新时间';
COMMENT ON COLUMN notification_templates.deleted_at IS '软删除时间';

-- =========================================
-- 添加列注释 - notification_subscriptions
-- =========================================

COMMENT ON COLUMN notification_subscriptions.id IS '主键ID';
COMMENT ON COLUMN notification_subscriptions.user_id IS '用户ID (关联auth_service.users.id)';
COMMENT ON COLUMN notification_subscriptions.email_enabled IS '邮件通知开关';
COMMENT ON COLUMN notification_subscriptions.sms_enabled IS '短信通知开关';
COMMENT ON COLUMN notification_subscriptions.notification_enabled IS '站内信通知开关';
COMMENT ON COLUMN notification_subscriptions.subscribed_types IS '订阅的通知类型 (JSONB数组, 例如: ["SYSTEM", "ACTIVITY"])';
COMMENT ON COLUMN notification_subscriptions.created_at IS '创建时间';
COMMENT ON COLUMN notification_subscriptions.updated_at IS '更新时间';

-- =========================================
-- 插入初始模板数据
-- =========================================

INSERT INTO notification_templates (template_code, template_name, template_type, subject, content, variables, status) VALUES
-- 1. 欢迎消息模板 (站内信)
('WELCOME', '欢迎消息', 'NOTIFICATION', '欢迎加入国创睿峰智能图书馆',
'尊敬的{{username}},您好!\n\n欢迎使用国创睿峰智能图书馆管理系统。系统已为您开通账号,您可以开始使用图书借阅、预约等服务。\n\n如有任何问题,请联系管理员。',
'["username"]', 'ACTIVE'),

-- 2. 验证码模板 (短信)
('VERIFICATION_CODE', '验证码短信', 'SMS', NULL,
'【国创睿峰】您的验证码是: {{code}},有效期{{expireTime}}分钟。如非本人操作,请忽略本短信。',
'["code", "expireTime"]', 'ACTIVE'),

-- 3. 借阅到期提醒 (邮件)
('BORROW_REMINDER', '借阅到期提醒', 'EMAIL', '图书即将到期提醒',
'尊敬的{{username}}:\n\n您借阅的图书《{{bookTitle}}》将于{{dueDate}}到期,请及时归还或续借。\n\n逾期归还将产生滞纳金,请注意!\n\n国创睿峰智能图书馆\n{{systemDate}}',
'["username", "bookTitle", "dueDate", "systemDate"]', 'ACTIVE'),

-- 4. 预约成功通知 (站内信)
('RESERVE_SUCCESS', '预约成功', 'NOTIFICATION', '图书预约成功通知',
'您好,{{username}}!\n\n您预约的图书《{{bookTitle}}》已成功,预约单号: {{reserveNo}}。\n\n图书到馆后我们会通知您取书,请保持手机畅通。预约有效期为{{validDays}}天。',
'["username", "bookTitle", "reserveNo", "validDays"]', 'ACTIVE'),

-- 5. 逾期通知 (邮件+站内信)
('OVERDUE_NOTICE', '图书逾期通知', 'EMAIL', '图书逾期通知 - 请尽快归还',
'{{username}},您好:\n\n您借阅的图书《{{bookTitle}}》已逾期{{overdueDays}}天,产生滞纳金{{fine}}元。\n\n请尽快到馆归还图书并缴纳滞纳金,以免影响您的信用记录。\n\n联系电话: {{contactPhone}}\n\n国创睿峰智能图书馆',
'["username", "bookTitle", "overdueDays", "fine", "contactPhone"]', 'ACTIVE')

ON CONFLICT (template_code) DO NOTHING;

-- =========================================
-- 数据验证
-- =========================================

-- 验证模板数据
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM notification_templates) < 5 THEN
        RAISE NOTICE '警告: 初始模板数据插入不完整,请检查';
    ELSE
        RAISE NOTICE '成功: 已插入 % 条初始模板数据', (SELECT COUNT(*) FROM notification_templates);
    END IF;
END $$;

-- =========================================
-- 脚本执行完成
-- =========================================
