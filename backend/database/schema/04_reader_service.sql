-- =========================================
-- 读者服务数据库表结构
-- Database: reader_service
-- Description: 读者管理相关表
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建reader_service数据库
-- CREATE DATABASE reader_service;
-- 连接到reader_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 读者表
CREATE TABLE IF NOT EXISTS readers (
    id BIGSERIAL PRIMARY KEY,
    reader_id VARCHAR(50) UNIQUE NOT NULL -- '读者证号(学号/工号)',
    card_number VARCHAR(50) UNIQUE -- '卡号',
    reader_name VARCHAR(100) NOT NULL -- '姓名',
    reader_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT' -- '读者类型: STUDENT/TEACHER/GUEST',
    gender VARCHAR(10) -- '性别: MALE/FEMALE/OTHER',
    id_card VARCHAR(18) UNIQUE -- '身份证号',
    phone VARCHAR(20) -- '手机号',
    email VARCHAR(100) -- '邮箱',
    department VARCHAR(200) -- '院系/部门',
    major VARCHAR(200) -- '专业',
    grade VARCHAR(20) -- '年级',
    class_name VARCHAR(50) -- '班级',
    student_type VARCHAR(20) -- '学生类型: UNDERGRADUATE/GRADUATE/DOCTORAL',
    photo_url VARCHAR(500) -- '照片URL',
    face_features TEXT -- '人脸特征(Base64)',
    deposit_amount DECIMAL(10,2) DEFAULT 0 -- '押金金额',
    credit_score INT DEFAULT 100 -- '信用分(0-100)',
    max_borrow_quantity INT DEFAULT 10 -- '最大借阅数量',
    current_borrow_count INT DEFAULT 0 -- '当前借阅数量',
    total_borrow_count INT DEFAULT 0 -- '累计借阅次数',
    overdue_count INT DEFAULT 0 -- '逾期次数',
    card_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' -- '证件状态: NORMAL/FROZEN/LOST/EXPIRED/CANCELLED',
    account_balance DECIMAL(10,2) DEFAULT 0 -- '账户余额',
    issue_date DATE -- '发证日期',
    expire_date DATE -- '证件有效期',
    remarks TEXT -- '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- '软删除时间'
);

-- 2. 读者证办理记录表
CREATE TABLE IF NOT EXISTS card_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL REFERENCES readers(id) ON DELETE CASCADE,
    record_type VARCHAR(20) NOT NULL -- '记录类型: ISSUE/RENEW/REISSUE/CANCEL',
    old_card_number VARCHAR(50) -- '旧卡号',
    new_card_number VARCHAR(50) -- '新卡号',
    fee_amount DECIMAL(10,2) DEFAULT 0 -- '费用',
    payment_status VARCHAR(20) DEFAULT 'PAID' -- '支付状态: UNPAID/PAID',
    payment_method VARCHAR(20) -- '支付方式: CASH/WECHAT/ALIPAY/BANK',
    reason TEXT -- '办理原因',
    operator_id BIGINT -- '操作员ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 读者行为日志表
CREATE TABLE IF NOT EXISTS reader_behavior_logs (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL -- '读者ID',
    behavior_type VARCHAR(50) NOT NULL -- '行为类型: SEARCH/VIEW/BORROW/RETURN/RESERVE/REVIEW',
    target_type VARCHAR(50) -- '目标类型: BOOK/CATEGORY/TAG',
    target_id BIGINT -- '目标ID',
    behavior_detail JSONB -- '行为详情(JSON)',
    ip_address VARCHAR(50) -- 'IP地址',
    user_agent VARCHAR(500) -- 'User Agent',
    session_id VARCHAR(100) -- '会话ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 读者收藏表
CREATE TABLE IF NOT EXISTS reader_favorites (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL -- '读者ID',
    book_id BIGINT NOT NULL -- '图书ID',
    folder_name VARCHAR(100) DEFAULT '默认收藏夹' -- '收藏夹名称',
    notes TEXT -- '笔记',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(reader_id, book_id)
);

-- 5. 读者评价表
CREATE TABLE IF NOT EXISTS reader_reviews (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL -- '读者ID',
    book_id BIGINT NOT NULL -- '图书ID',
    rating INT NOT NULL -- '评分(1-5星)',
    review_title VARCHAR(200) -- '评价标题',
    review_content TEXT -- '评价内容',
    is_anonymous BOOLEAN DEFAULT FALSE -- '是否匿名',
    helpful_count INT DEFAULT 0 -- '有用数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' -- '状态: PENDING/APPROVED/REJECTED',
    reviewed_by BIGINT -- '审核人ID',
    reviewed_at TIMESTAMP -- '审核时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 读者通知表
CREATE TABLE IF NOT EXISTS reader_notifications (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL -- '读者ID',
    notification_type VARCHAR(50) NOT NULL -- '通知类型: OVERDUE/RESERVATION/FINE/SYSTEM',
    title VARCHAR(200) NOT NULL -- '通知标题',
    content TEXT NOT NULL -- '通知内容',
    priority VARCHAR(20) DEFAULT 'NORMAL' -- '优先级: LOW/NORMAL/HIGH/URGENT',
    send_channel VARCHAR(50) -- '发送渠道: EMAIL/SMS/WECHAT/APP',
    is_read BOOLEAN DEFAULT FALSE -- '是否已读',
    read_time TIMESTAMP -- '阅读时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_readers_reader_id ON readers(reader_id);
CREATE INDEX idx_readers_card_number ON readers(card_number);
CREATE INDEX idx_readers_phone ON readers(phone);
CREATE INDEX idx_readers_email ON readers(email);
CREATE INDEX idx_readers_reader_type ON readers(reader_type);
CREATE INDEX idx_readers_card_status ON readers(card_status);
CREATE INDEX idx_card_records_reader_id ON card_records(reader_id);
CREATE INDEX idx_reader_behavior_logs_reader_id ON reader_behavior_logs(reader_id);
CREATE INDEX idx_reader_behavior_logs_created_at ON reader_behavior_logs(created_at);
CREATE INDEX idx_reader_favorites_reader_id ON reader_favorites(reader_id);
CREATE INDEX idx_reader_favorites_book_id ON reader_favorites(book_id);
CREATE INDEX idx_reader_reviews_reader_id ON reader_reviews(reader_id);
CREATE INDEX idx_reader_reviews_book_id ON reader_reviews(book_id);
CREATE INDEX idx_reader_notifications_reader_id ON reader_notifications(reader_id);
CREATE INDEX idx_reader_notifications_is_read ON reader_notifications(is_read);

-- 添加表注释
-- ON TABLE readers IS '读者表';
-- ON TABLE card_records IS '读者证办理记录表';
-- ON TABLE reader_behavior_logs IS '读者行为日志表';
-- ON TABLE reader_favorites IS '读者收藏表';
-- ON TABLE reader_reviews IS '读者评价表';
-- ON TABLE reader_notifications IS '读者通知表';
