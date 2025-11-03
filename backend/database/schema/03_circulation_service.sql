-- =========================================
-- 流通服务数据库表结构
-- Database: circulation_service
-- Description: 借阅流通相关表
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建circulation_service数据库
-- CREATE DATABASE circulation_service;
-- 连接到circulation_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 借阅记录表
CREATE TABLE IF NOT EXISTS circulation_records (
    id BIGSERIAL PRIMARY KEY,
    record_code VARCHAR(50) UNIQUE NOT NULL -- '借阅记录编号',
    book_item_id BIGINT NOT NULL -- '馆藏ID',
    book_id BIGINT NOT NULL -- '图书ID',
    reader_id BIGINT NOT NULL -- '读者ID',
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- '借阅日期',
    due_date TIMESTAMP NOT NULL -- '应还日期',
    return_date TIMESTAMP -- '归还日期',
    renew_count INT DEFAULT 0 -- '续借次数',
    renew_limit INT DEFAULT 3 -- '续借次数限制',
    overdue_days INT DEFAULT 0 -- '逾期天数',
    fine_amount DECIMAL(10,2) DEFAULT 0 -- '罚款金额',
    fine_paid BOOLEAN DEFAULT FALSE -- '罚款是否已缴',
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED' -- '状态: BORROWED/RETURNED/OVERDUE/LOST',
    borrow_library VARCHAR(100) -- '借阅馆',
    return_library VARCHAR(100) -- '归还馆',
    operator_id BIGINT -- '操作员ID',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 预约记录表
CREATE TABLE IF NOT EXISTS reservation_records (
    id BIGSERIAL PRIMARY KEY,
    reservation_code VARCHAR(50) UNIQUE NOT NULL -- '预约编号',
    book_id BIGINT NOT NULL -- '图书ID',
    reader_id BIGINT NOT NULL -- '读者ID',
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- '预约日期',
    expire_date TIMESTAMP NOT NULL -- '预约失效日期',
    notification_sent BOOLEAN DEFAULT FALSE -- '是否已通知',
    notification_time TIMESTAMP -- '通知时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' -- '状态: PENDING/NOTIFIED/PICKED_UP/EXPIRED/CANCELLED',
    cancel_reason VARCHAR(200) -- '取消原因',
    picked_up_time TIMESTAMP -- '取书时间',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 续借记录表
CREATE TABLE IF NOT EXISTS renew_records (
    id BIGSERIAL PRIMARY KEY,
    circulation_record_id BIGINT NOT NULL REFERENCES circulation_records(id) ON DELETE CASCADE,
    old_due_date TIMESTAMP NOT NULL -- '原应还日期',
    new_due_date TIMESTAMP NOT NULL -- '新应还日期',
    renew_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- '续借日期',
    operator_id BIGINT -- '操作员ID',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 罚款记录表
CREATE TABLE IF NOT EXISTS fine_records (
    id BIGSERIAL PRIMARY KEY,
    fine_code VARCHAR(50) UNIQUE NOT NULL -- '罚款编号',
    circulation_record_id BIGINT REFERENCES circulation_records(id) ON DELETE SET NULL,
    reader_id BIGINT NOT NULL -- '读者ID',
    fine_type VARCHAR(20) NOT NULL -- '罚款类型: OVERDUE/LOST/DAMAGE',
    fine_amount DECIMAL(10,2) NOT NULL -- '罚款金额',
    paid_amount DECIMAL(10,2) DEFAULT 0 -- '已缴金额',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' -- '缴费状态: UNPAID/PARTIAL/PAID',
    payment_method VARCHAR(20) -- '支付方式: CASH/WECHAT/ALIPAY/BANK',
    payment_time TIMESTAMP -- '支付时间',
    payment_operator_id BIGINT -- '收款操作员ID',
    description TEXT -- '罚款说明',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. 借阅规则表
CREATE TABLE IF NOT EXISTS circulation_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL -- '规则名称',
    reader_type VARCHAR(20) NOT NULL -- '读者类型: STUDENT/TEACHER/ADMIN',
    book_category VARCHAR(50) -- '图书分类',
    max_borrow_quantity INT DEFAULT 10 -- '最大借阅数量',
    borrow_period_days INT DEFAULT 30 -- '借阅期限(天)',
    renew_limit INT DEFAULT 3 -- '续借次数限制',
    renew_period_days INT DEFAULT 30 -- '续借期限(天)',
    overdue_fine_per_day DECIMAL(10,2) DEFAULT 0.10 -- '逾期罚款(元/天)',
    lost_compensation_rate DECIMAL(4,2) DEFAULT 3.00 -- '丢失赔偿倍率',
    can_reserve BOOLEAN DEFAULT TRUE -- '是否可预约',
    is_active BOOLEAN DEFAULT TRUE -- '是否启用',
    priority INT DEFAULT 0 -- '优先级',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 违规记录表
CREATE TABLE IF NOT EXISTS violation_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL -- '读者ID',
    violation_type VARCHAR(20) NOT NULL -- '违规类型: OVERDUE/DAMAGE/LOST/OTHER',
    violation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- '违规日期',
    circulation_record_id BIGINT REFERENCES circulation_records(id) ON DELETE SET NULL,
    description TEXT -- '违规描述',
    penalty_points INT DEFAULT 0 -- '扣罚分数',
    penalty_amount DECIMAL(10,2) DEFAULT 0 -- '罚款金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' -- '处理状态: PENDING/RESOLVED',
    handler_id BIGINT -- '处理人ID',
    handle_time TIMESTAMP -- '处理时间',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_circulation_records_book_item_id ON circulation_records(book_item_id);
CREATE INDEX idx_circulation_records_book_id ON circulation_records(book_id);
CREATE INDEX idx_circulation_records_reader_id ON circulation_records(reader_id);
CREATE INDEX idx_circulation_records_status ON circulation_records(status);
CREATE INDEX idx_circulation_records_borrow_date ON circulation_records(borrow_date);
CREATE INDEX idx_circulation_records_due_date ON circulation_records(due_date);
CREATE INDEX idx_reservation_records_book_id ON reservation_records(book_id);
CREATE INDEX idx_reservation_records_reader_id ON reservation_records(reader_id);
CREATE INDEX idx_reservation_records_status ON reservation_records(status);
CREATE INDEX idx_renew_records_circulation_record_id ON renew_records(circulation_record_id);
CREATE INDEX idx_fine_records_reader_id ON fine_records(reader_id);
CREATE INDEX idx_fine_records_payment_status ON fine_records(payment_status);
CREATE INDEX idx_violation_records_reader_id ON violation_records(reader_id);
CREATE INDEX idx_violation_records_status ON violation_records(status);

-- 插入默认借阅规则
INSERT INTO circulation_rules (rule_name, reader_type, max_borrow_quantity, borrow_period_days, renew_limit, overdue_fine_per_day) VALUES
('本科生借阅规则', 'STUDENT', 10, 30, 2, 0.10),
('研究生借阅规则', 'STUDENT', 15, 60, 3, 0.10),
('教师借阅规则', 'TEACHER', 30, 90, 5, 0.05),
('管理员借阅规则', 'ADMIN', 50, 180, 10, 0.00)
ON CONFLICT DO NOTHING;

-- 添加表注释
-- ON TABLE circulation_records IS '借阅记录表';
-- ON TABLE reservation_records IS '预约记录表';
-- ON TABLE renew_records IS '续借记录表';
-- ON TABLE fine_records IS '罚款记录表';
-- ON TABLE circulation_rules IS '借阅规则表';
-- ON TABLE violation_records IS '违规记录表';
