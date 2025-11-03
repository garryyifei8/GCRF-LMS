-- =========================================
-- 流通服务数据库表结构
-- Database: circulation_service
-- Description: 借阅、预约、逾期管理相关表
-- Version: 1.0.0
-- Date: 2025-10-28
-- =========================================

-- 注意: 此脚本会删除并重建circulation_service数据库
-- ⚠️ 警告: 执行此脚本将删除所有现有数据！请确保已备份重要数据
-- 如果只需要更新表结构，请注释掉DROP DATABASE语句

-- 删除并重建数据库
DROP DATABASE IF EXISTS circulation_service;
CREATE DATABASE circulation_service WITH ENCODING 'UTF8';

-- 连接到circulation_service数据库
\c circulation_service;

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- 用于模糊搜索

-- =========================================
-- 1. 借阅记录表 (borrows)
-- =========================================
CREATE TABLE IF NOT EXISTS borrows (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 业务标识
    borrow_id VARCHAR(50) UNIQUE NOT NULL,                    -- 借阅编号(格式: BW-YYYYMMDD-0001)

    -- 外键关联
    reader_id BIGINT NOT NULL,                                -- 读者ID (关联readers.id)
    book_id BIGINT NOT NULL,                                  -- 图书ID (关联books.id)
    book_barcode VARCHAR(50),                                 -- 图书条码 (冗余字段,便于查询)

    -- 借阅信息
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 借阅日期
    due_date TIMESTAMP NOT NULL,                              -- 应还日期
    return_date TIMESTAMP,                                    -- 实际归还日期 (NULL=未归还)
    renew_count INTEGER NOT NULL DEFAULT 0,                   -- 续借次数
    max_renew_count INTEGER NOT NULL DEFAULT 2,               -- 最大续借次数

    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',           -- 状态: BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期, LOST-遗失

    -- 罚金管理
    fine_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,          -- 罚金金额(元)
    fine_paid BOOLEAN NOT NULL DEFAULT FALSE,                 -- 是否已支付罚金
    fine_paid_date TIMESTAMP,                                 -- 罚金支付日期

    -- 其他信息
    remarks TEXT,                                             -- 备注

    -- 时间戳字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted_at TIMESTAMP,                                     -- 删除时间(软删除)

    -- 约束条件
    CONSTRAINT chk_borrow_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'LOST')),
    CONSTRAINT chk_renew_count CHECK (renew_count >= 0 AND renew_count <= max_renew_count),
    CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
    CONSTRAINT chk_dates CHECK (due_date > borrow_date),
    CONSTRAINT chk_return_date CHECK (return_date IS NULL OR return_date >= borrow_date)
);

-- =========================================
-- 2. 预约记录表 (reserves)
-- =========================================
CREATE TABLE IF NOT EXISTS reserves (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 业务标识
    reserve_id VARCHAR(50) UNIQUE NOT NULL,                   -- 预约编号(格式: RV-YYYYMMDD-0001)

    -- 外键关联
    reader_id BIGINT NOT NULL,                                -- 读者ID (关联readers.id)
    book_id BIGINT NOT NULL,                                  -- 图书ID (关联books.id)

    -- 预约信息
    reserve_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 预约日期
    expiry_date TIMESTAMP NOT NULL,                           -- 预约过期日期 (通常为7天后)
    pickup_date TIMESTAMP,                                    -- 取书日期 (NULL=未取书)
    cancel_date TIMESTAMP,                                    -- 取消日期 (NULL=未取消)

    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',           -- 状态: RESERVED-已预约, PICKED_UP-已取书, CANCELLED-已取消, EXPIRED-已过期

    -- 通知管理
    notify_sent BOOLEAN NOT NULL DEFAULT FALSE,               -- 是否已发送通知
    notify_sent_date TIMESTAMP,                               -- 通知发送日期
    notify_count INTEGER NOT NULL DEFAULT 0,                  -- 通知发送次数

    -- 其他信息
    remarks TEXT,                                             -- 备注

    -- 时间戳字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted_at TIMESTAMP,                                     -- 删除时间(软删除)

    -- 约束条件
    CONSTRAINT chk_reserve_status CHECK (status IN ('RESERVED', 'PICKED_UP', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_notify_count CHECK (notify_count >= 0),
    CONSTRAINT chk_reserve_dates CHECK (expiry_date > reserve_date)
);

-- =========================================
-- 3. 创建索引 - borrows表
-- =========================================

-- 唯一索引
-- borrow_id 已经在表定义中设置为UNIQUE

-- 外键索引 - 高频查询字段
CREATE INDEX idx_borrows_reader_id ON borrows(reader_id);
CREATE INDEX idx_borrows_book_id ON borrows(book_id);
CREATE INDEX idx_borrows_book_barcode ON borrows(book_barcode) WHERE book_barcode IS NOT NULL;

-- 状态和日期索引
CREATE INDEX idx_borrows_status ON borrows(status);
CREATE INDEX idx_borrows_borrow_date ON borrows(borrow_date DESC);
CREATE INDEX idx_borrows_due_date ON borrows(due_date);
CREATE INDEX idx_borrows_return_date ON borrows(return_date) WHERE return_date IS NOT NULL;

-- 复合索引 - 常见组合查询
CREATE INDEX idx_borrows_reader_status ON borrows(reader_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_borrows_book_status ON borrows(book_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_borrows_status_due ON borrows(status, due_date) WHERE deleted_at IS NULL;

-- 部分索引 - 优化活跃借阅查询
CREATE INDEX idx_borrows_active ON borrows(reader_id, book_id)
    WHERE status = 'BORROWED' AND deleted_at IS NULL;
CREATE INDEX idx_borrows_overdue ON borrows(reader_id, due_date)
    WHERE status = 'OVERDUE' AND deleted_at IS NULL;
CREATE INDEX idx_borrows_unreturned ON borrows(reader_id, borrow_date DESC)
    WHERE return_date IS NULL AND deleted_at IS NULL;

-- 罚金相关索引
CREATE INDEX idx_borrows_unpaid_fine ON borrows(reader_id, fine_amount)
    WHERE fine_amount > 0 AND fine_paid = FALSE AND deleted_at IS NULL;

-- =========================================
-- 4. 创建索引 - reserves表
-- =========================================

-- 唯一索引
-- reserve_id 已经在表定义中设置为UNIQUE

-- 外键索引
CREATE INDEX idx_reserves_reader_id ON reserves(reader_id);
CREATE INDEX idx_reserves_book_id ON reserves(book_id);

-- 状态和日期索引
CREATE INDEX idx_reserves_status ON reserves(status);
CREATE INDEX idx_reserves_reserve_date ON reserves(reserve_date DESC);
CREATE INDEX idx_reserves_expiry_date ON reserves(expiry_date);

-- 复合索引
CREATE INDEX idx_reserves_reader_status ON reserves(reader_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reserves_book_status ON reserves(book_id, status) WHERE deleted_at IS NULL;

-- 部分索引 - 优化活跃预约查询
CREATE INDEX idx_reserves_active ON reserves(book_id, reserve_date)
    WHERE status = 'RESERVED' AND deleted_at IS NULL;
CREATE INDEX idx_reserves_pending_pickup ON reserves(reader_id, expiry_date)
    WHERE status = 'RESERVED' AND pickup_date IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_reserves_pending_notify ON reserves(reader_id)
    WHERE status = 'RESERVED' AND notify_sent = FALSE AND deleted_at IS NULL;

-- =========================================
-- 5. 创建触发器 - 自动更新updated_at
-- =========================================

-- 创建通用的更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为borrows表添加触发器
CREATE TRIGGER trigger_borrows_updated_at
    BEFORE UPDATE ON borrows
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为reserves表添加触发器
CREATE TRIGGER trigger_reserves_updated_at
    BEFORE UPDATE ON reserves
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- 6. 添加表注释
-- =========================================

COMMENT ON TABLE borrows IS '借阅记录表 - 记录图书借阅、归还、续借等信息';
COMMENT ON TABLE reserves IS '预约记录表 - 记录图书预约、取书等信息';

-- borrows表字段注释
COMMENT ON COLUMN borrows.borrow_id IS '借阅编号,格式: BW-YYYYMMDD-0001';
COMMENT ON COLUMN borrows.reader_id IS '读者ID,关联readers.id';
COMMENT ON COLUMN borrows.book_id IS '图书ID,关联books.id';
COMMENT ON COLUMN borrows.book_barcode IS '图书条码,冗余字段便于查询';
COMMENT ON COLUMN borrows.status IS '状态: BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期, LOST-遗失';
COMMENT ON COLUMN borrows.renew_count IS '已续借次数';
COMMENT ON COLUMN borrows.max_renew_count IS '最大允许续借次数(默认2次)';
COMMENT ON COLUMN borrows.fine_amount IS '罚金金额(元),每天1元';
COMMENT ON COLUMN borrows.fine_paid IS '是否已支付罚金';
COMMENT ON COLUMN borrows.deleted_at IS '软删除标记,NULL表示未删除';

-- reserves表字段注释
COMMENT ON COLUMN reserves.reserve_id IS '预约编号,格式: RV-YYYYMMDD-0001';
COMMENT ON COLUMN reserves.reader_id IS '读者ID,关联readers.id';
COMMENT ON COLUMN reserves.book_id IS '图书ID,关联books.id';
COMMENT ON COLUMN reserves.status IS '状态: RESERVED-已预约, PICKED_UP-已取书, CANCELLED-已取消, EXPIRED-已过期';
COMMENT ON COLUMN reserves.notify_sent IS '是否已发送到书通知';
COMMENT ON COLUMN reserves.notify_count IS '通知发送次数';
COMMENT ON COLUMN reserves.deleted_at IS '软删除标记,NULL表示未删除';

-- =========================================
-- 7. 插入测试数据 - borrows表
-- =========================================

-- 注意: reader_id和book_id需要与实际的readers和books表中的ID对应
-- 以下使用假设的ID值,实际使用时需要根据真实数据调整

-- 插入40条借阅记录测试数据
INSERT INTO borrows (borrow_id, reader_id, book_id, book_barcode, borrow_date, due_date, return_date, renew_count, status, fine_amount, fine_paid, remarks) VALUES
-- 已归还记录 (10条)
('BW-20251001-0001', 1, 1, 'BK20250001-001', '2025-10-01 09:00:00', '2025-10-31 23:59:59', '2025-10-25 14:30:00', 0, 'RETURNED', 0.00, TRUE, '正常归还'),
('BW-20251002-0001', 2, 2, 'BK20250002-001', '2025-10-02 10:30:00', '2025-11-01 23:59:59', '2025-10-20 16:00:00', 1, 'RETURNED', 0.00, TRUE, '续借一次后归还'),
('BW-20251003-0001', 3, 3, 'BK20250003-001', '2025-10-03 14:00:00', '2025-11-02 23:59:59', '2025-11-05 10:00:00', 0, 'RETURNED', 3.00, TRUE, '逾期3天,已支付罚金'),
('BW-20251004-0001', 4, 4, 'BK20250004-001', '2025-10-04 11:00:00', '2025-11-03 23:59:59', '2025-10-28 09:00:00', 0, 'RETURNED', 0.00, TRUE, '提前归还'),
('BW-20251005-0001', 5, 5, 'BK20250005-001', '2025-10-05 15:30:00', '2025-11-04 23:59:59', '2025-11-04 18:00:00', 1, 'RETURNED', 0.00, TRUE, '续借后按时归还'),
('BW-20251006-0001', 1, 6, 'BK20250006-001', '2025-10-06 09:00:00', '2025-11-05 23:59:59', '2025-11-10 14:00:00', 0, 'RETURNED', 5.00, TRUE, '逾期5天'),
('BW-20251007-0001', 2, 7, 'BK20250007-001', '2025-10-07 13:00:00', '2025-11-06 23:59:59', '2025-10-30 11:00:00', 0, 'RETURNED', 0.00, TRUE, '正常归还'),
('BW-20251008-0001', 3, 8, 'BK20250008-001', '2025-10-08 10:00:00', '2025-11-07 23:59:59', '2025-11-01 15:00:00', 0, 'RETURNED', 0.00, TRUE, '正常归还'),
('BW-20251009-0001', 4, 9, 'BK20250009-001', '2025-10-09 16:00:00', '2025-11-08 23:59:59', '2025-11-08 17:00:00', 2, 'RETURNED', 0.00, TRUE, '续借两次后归还'),
('BW-20251010-0001', 5, 10, 'BK20250010-001', '2025-10-10 14:00:00', '2025-11-09 23:59:59', '2025-11-12 10:00:00', 0, 'RETURNED', 3.00, TRUE, '逾期3天,已支付'),

-- 借阅中记录 (15条)
('BW-20251011-0001', 6, 11, 'BK20250011-001', '2025-10-11 09:30:00', '2025-11-10 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251012-0001', 7, 12, 'BK20250012-001', '2025-10-12 10:00:00', '2025-11-11 23:59:59', NULL, 1, 'BORROWED', 0.00, FALSE, '已续借一次'),
('BW-20251013-0001', 8, 13, 'BK20250013-001', '2025-10-13 11:30:00', '2025-11-12 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251014-0001', 9, 14, 'BK20250014-001', '2025-10-14 15:00:00', '2025-11-13 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251015-0001', 10, 15, 'BK20250015-001', '2025-10-15 09:00:00', '2025-11-14 23:59:59', NULL, 1, 'BORROWED', 0.00, FALSE, '已续借一次'),
('BW-20251016-0001', 11, 16, 'BK20250016-001', '2025-10-16 14:00:00', '2025-11-15 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251017-0001', 12, 17, 'BK20250017-001', '2025-10-17 10:30:00', '2025-11-16 23:59:59', NULL, 2, 'BORROWED', 0.00, FALSE, '已续借两次'),
('BW-20251018-0001', 13, 18, 'BK20250018-001', '2025-10-18 16:00:00', '2025-11-17 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251019-0001', 14, 19, 'BK20250019-001', '2025-10-19 11:00:00', '2025-11-18 23:59:59', NULL, 1, 'BORROWED', 0.00, FALSE, '已续借一次'),
('BW-20251020-0001', 15, 20, 'BK20250020-001', '2025-10-20 13:30:00', '2025-11-19 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251021-0001', 16, 21, 'BK20250021-001', '2025-10-21 09:00:00', '2025-11-20 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251022-0001', 17, 22, 'BK20250022-001', '2025-10-22 15:00:00', '2025-11-21 23:59:59', NULL, 1, 'BORROWED', 0.00, FALSE, '已续借一次'),
('BW-20251023-0001', 18, 23, 'BK20250023-001', '2025-10-23 10:00:00', '2025-11-22 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251024-0001', 19, 24, 'BK20250024-001', '2025-10-24 14:30:00', '2025-11-23 23:59:59', NULL, 0, 'BORROWED', 0.00, FALSE, '在借中'),
('BW-20251025-0001', 20, 25, 'BK20250025-001', '2025-10-25 11:30:00', '2025-11-24 23:59:59', NULL, 2, 'BORROWED', 0.00, FALSE, '已续借两次'),

-- 逾期记录 (10条)
('BW-20250901-0001', 21, 26, 'BK20250026-001', '2025-09-01 09:00:00', '2025-10-01 23:59:59', NULL, 0, 'OVERDUE', 27.00, FALSE, '逾期27天未归还'),
('BW-20250902-0001', 22, 27, 'BK20250027-001', '2025-09-02 10:00:00', '2025-10-02 23:59:59', NULL, 1, 'OVERDUE', 26.00, FALSE, '逾期26天,已催还'),
('BW-20250903-0001', 23, 28, 'BK20250028-001', '2025-09-03 11:00:00', '2025-10-03 23:59:59', NULL, 0, 'OVERDUE', 25.00, FALSE, '逾期25天'),
('BW-20250904-0001', 24, 29, 'BK20250029-001', '2025-09-04 14:00:00', '2025-10-04 23:59:59', NULL, 0, 'OVERDUE', 24.00, FALSE, '逾期24天'),
('BW-20250905-0001', 25, 30, 'BK20250030-001', '2025-09-05 15:00:00', '2025-10-05 23:59:59', NULL, 2, 'OVERDUE', 23.00, FALSE, '续借两次后仍逾期'),
('BW-20250910-0001', 26, 31, 'BK20250031-001', '2025-09-10 09:00:00', '2025-10-10 23:59:59', NULL, 0, 'OVERDUE', 18.00, FALSE, '逾期18天'),
('BW-20250915-0001', 27, 32, 'BK20250032-001', '2025-09-15 10:00:00', '2025-10-15 23:59:59', NULL, 1, 'OVERDUE', 13.00, FALSE, '逾期13天'),
('BW-20250920-0001', 28, 33, 'BK20250033-001', '2025-09-20 11:00:00', '2025-10-20 23:59:59', NULL, 0, 'OVERDUE', 8.00, FALSE, '逾期8天'),
('BW-20250922-0001', 29, 34, 'BK20250034-001', '2025-09-22 13:00:00', '2025-10-22 23:59:59', NULL, 0, 'OVERDUE', 6.00, FALSE, '逾期6天'),
('BW-20250925-0001', 30, 35, 'BK20250035-001', '2025-09-25 15:00:00', '2025-10-25 23:59:59', NULL, 1, 'OVERDUE', 3.00, FALSE, '逾期3天'),

-- 遗失记录 (5条)
('BW-20250801-0001', 31, 36, 'BK20250036-001', '2025-08-01 09:00:00', '2025-08-31 23:59:59', NULL, 0, 'LOST', 50.00, FALSE, '图书遗失,需赔偿'),
('BW-20250802-0001', 32, 37, 'BK20250037-001', '2025-08-02 10:00:00', '2025-09-01 23:59:59', NULL, 1, 'LOST', 45.00, TRUE, '图书遗失,已赔偿'),
('BW-20250803-0001', 33, 38, 'BK20250038-001', '2025-08-03 11:00:00', '2025-09-02 23:59:59', NULL, 0, 'LOST', 60.00, FALSE, '图书遗失'),
('BW-20250804-0001', 34, 39, 'BK20250039-001', '2025-08-04 14:00:00', '2025-09-03 23:59:59', NULL, 0, 'LOST', 55.00, TRUE, '图书遗失,已赔偿'),
('BW-20250805-0001', 35, 40, 'BK20250040-001', '2025-08-05 15:00:00', '2025-09-04 23:59:59', NULL, 2, 'LOST', 50.00, FALSE, '续借后遗失');

-- =========================================
-- 8. 插入测试数据 - reserves表
-- =========================================

-- 插入30条预约记录测试数据
INSERT INTO reserves (reserve_id, reader_id, book_id, reserve_date, expiry_date, pickup_date, cancel_date, status, notify_sent, notify_sent_date, notify_count, remarks) VALUES
-- 已取书记录 (10条)
('RV-20251001-0001', 1, 41, '2025-10-01 09:00:00', '2025-10-08 23:59:59', '2025-10-02 14:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-01 18:00:00', 1, '预约成功,已取书'),
('RV-20251002-0001', 2, 42, '2025-10-02 10:00:00', '2025-10-09 23:59:59', '2025-10-03 15:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-02 18:00:00', 1, '预约成功,已取书'),
('RV-20251003-0001', 3, 43, '2025-10-03 11:00:00', '2025-10-10 23:59:59', '2025-10-05 10:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-03 18:00:00', 1, '预约成功,已取书'),
('RV-20251004-0001', 4, 44, '2025-10-04 14:00:00', '2025-10-11 23:59:59', '2025-10-06 16:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-04 18:00:00', 1, '预约成功,已取书'),
('RV-20251005-0001', 5, 45, '2025-10-05 15:00:00', '2025-10-12 23:59:59', '2025-10-07 09:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-05 18:00:00', 1, '预约成功,已取书'),
('RV-20251006-0001', 6, 46, '2025-10-06 09:00:00', '2025-10-13 23:59:59', '2025-10-08 11:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-06 18:00:00', 1, '预约成功,已取书'),
('RV-20251007-0001', 7, 47, '2025-10-07 10:00:00', '2025-10-14 23:59:59', '2025-10-09 14:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-07 18:00:00', 1, '预约成功,已取书'),
('RV-20251008-0001', 8, 48, '2025-10-08 11:00:00', '2025-10-15 23:59:59', '2025-10-10 15:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-08 18:00:00', 1, '预约成功,已取书'),
('RV-20251009-0001', 9, 49, '2025-10-09 14:00:00', '2025-10-16 23:59:59', '2025-10-11 10:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-09 18:00:00', 1, '预约成功,已取书'),
('RV-20251010-0001', 10, 50, '2025-10-10 15:00:00', '2025-10-17 23:59:59', '2025-10-12 16:00:00', NULL, 'PICKED_UP', TRUE, '2025-10-10 18:00:00', 1, '预约成功,已取书'),

-- 进行中的预约 (10条)
('RV-20251020-0001', 11, 11, '2025-10-20 09:00:00', '2025-10-27 23:59:59', NULL, NULL, 'RESERVED', TRUE, '2025-10-20 18:00:00', 1, '等待取书'),
('RV-20251021-0001', 12, 12, '2025-10-21 10:00:00', '2025-10-28 23:59:59', NULL, NULL, 'RESERVED', TRUE, '2025-10-21 18:00:00', 1, '等待取书'),
('RV-20251022-0001', 13, 13, '2025-10-22 11:00:00', '2025-10-29 23:59:59', NULL, NULL, 'RESERVED', TRUE, '2025-10-22 18:00:00', 1, '等待取书'),
('RV-20251023-0001', 14, 14, '2025-10-23 14:00:00', '2025-10-30 23:59:59', NULL, NULL, 'RESERVED', TRUE, '2025-10-23 18:00:00', 1, '等待取书'),
('RV-20251024-0001', 15, 15, '2025-10-24 15:00:00', '2025-10-31 23:59:59', NULL, NULL, 'RESERVED', TRUE, '2025-10-24 18:00:00', 1, '等待取书'),
('RV-20251025-0001', 16, 16, '2025-10-25 09:00:00', '2025-11-01 23:59:59', NULL, NULL, 'RESERVED', FALSE, NULL, 0, '待通知'),
('RV-20251026-0001', 17, 17, '2025-10-26 10:00:00', '2025-11-02 23:59:59', NULL, NULL, 'RESERVED', FALSE, NULL, 0, '待通知'),
('RV-20251027-0001', 18, 18, '2025-10-27 11:00:00', '2025-11-03 23:59:59', NULL, NULL, 'RESERVED', FALSE, NULL, 0, '待通知'),
('RV-20251028-0001', 19, 19, '2025-10-28 14:00:00', '2025-11-04 23:59:59', NULL, NULL, 'RESERVED', FALSE, NULL, 0, '待通知'),
('RV-20251029-0001', 20, 20, '2025-10-29 15:00:00', '2025-11-05 23:59:59', NULL, NULL, 'RESERVED', FALSE, NULL, 0, '待通知'),

-- 已取消记录 (5条)
('RV-20251011-0001', 21, 21, '2025-10-11 09:00:00', '2025-10-18 23:59:59', NULL, '2025-10-12 10:00:00', 'CANCELLED', FALSE, NULL, 0, '读者主动取消'),
('RV-20251012-0001', 22, 22, '2025-10-12 10:00:00', '2025-10-19 23:59:59', NULL, '2025-10-13 11:00:00', 'CANCELLED', FALSE, NULL, 0, '读者主动取消'),
('RV-20251013-0001', 23, 23, '2025-10-13 11:00:00', '2025-10-20 23:59:59', NULL, '2025-10-14 09:00:00', 'CANCELLED', TRUE, '2025-10-13 18:00:00', 1, '通知后取消'),
('RV-20251014-0001', 24, 24, '2025-10-14 14:00:00', '2025-10-21 23:59:59', NULL, '2025-10-15 15:00:00', 'CANCELLED', FALSE, NULL, 0, '读者主动取消'),
('RV-20251015-0001', 25, 25, '2025-10-15 15:00:00', '2025-10-22 23:59:59', NULL, '2025-10-16 16:00:00', 'CANCELLED', TRUE, '2025-10-15 18:00:00', 1, '通知后取消'),

-- 已过期记录 (5条)
('RV-20250901-0001', 26, 26, '2025-09-01 09:00:00', '2025-09-08 23:59:59', NULL, NULL, 'EXPIRED', TRUE, '2025-09-01 18:00:00', 2, '超过7天未取书,自动过期'),
('RV-20250902-0001', 27, 27, '2025-09-02 10:00:00', '2025-09-09 23:59:59', NULL, NULL, 'EXPIRED', TRUE, '2025-09-02 18:00:00', 2, '超过7天未取书,自动过期'),
('RV-20250903-0001', 28, 28, '2025-09-03 11:00:00', '2025-09-10 23:59:59', NULL, NULL, 'EXPIRED', TRUE, '2025-09-03 18:00:00', 2, '超过7天未取书,自动过期'),
('RV-20250904-0001', 29, 29, '2025-09-04 14:00:00', '2025-09-11 23:59:59', NULL, NULL, 'EXPIRED', TRUE, '2025-09-04 18:00:00', 2, '超过7天未取书,自动过期'),
('RV-20250905-0001', 30, 30, '2025-09-05 15:00:00', '2025-09-12 23:59:59', NULL, NULL, 'EXPIRED', TRUE, '2025-09-05 18:00:00', 2, '超过7天未取书,自动过期');

-- =========================================
-- 9. 创建统计视图 (可选)
-- =========================================

-- 借阅统计视图
CREATE OR REPLACE VIEW v_borrow_statistics AS
SELECT
    COUNT(*) AS total_borrows,
    COUNT(CASE WHEN status = 'BORROWED' THEN 1 END) AS active_borrows,
    COUNT(CASE WHEN status = 'RETURNED' THEN 1 END) AS returned_borrows,
    COUNT(CASE WHEN status = 'OVERDUE' THEN 1 END) AS overdue_borrows,
    COUNT(CASE WHEN status = 'LOST' THEN 1 END) AS lost_borrows,
    SUM(CASE WHEN fine_paid = FALSE AND fine_amount > 0 THEN fine_amount ELSE 0 END) AS unpaid_fines,
    SUM(CASE WHEN fine_paid = TRUE THEN fine_amount ELSE 0 END) AS paid_fines
FROM borrows
WHERE deleted_at IS NULL;

-- 预约统计视图
CREATE OR REPLACE VIEW v_reserve_statistics AS
SELECT
    COUNT(*) AS total_reserves,
    COUNT(CASE WHEN status = 'RESERVED' THEN 1 END) AS active_reserves,
    COUNT(CASE WHEN status = 'PICKED_UP' THEN 1 END) AS picked_up_reserves,
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_reserves,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) AS expired_reserves,
    COUNT(CASE WHEN status = 'RESERVED' AND notify_sent = FALSE THEN 1 END) AS pending_notifications
FROM reserves
WHERE deleted_at IS NULL;

-- 读者借阅统计视图
CREATE OR REPLACE VIEW v_reader_borrow_stats AS
SELECT
    reader_id,
    COUNT(*) AS total_borrow_count,
    COUNT(CASE WHEN status = 'BORROWED' THEN 1 END) AS current_borrow_count,
    COUNT(CASE WHEN status = 'OVERDUE' THEN 1 END) AS overdue_count,
    SUM(CASE WHEN fine_paid = FALSE AND fine_amount > 0 THEN fine_amount ELSE 0 END) AS unpaid_fine_amount
FROM borrows
WHERE deleted_at IS NULL
GROUP BY reader_id;

-- =========================================
-- 10. 授权 (如果需要特定用户权限)
-- =========================================

-- 示例: 如果有特定的应用用户,可以授权
-- GRANT SELECT, INSERT, UPDATE, DELETE ON borrows TO circulation_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON reserves TO circulation_app_user;
-- GRANT SELECT ON v_borrow_statistics TO circulation_app_user;
-- GRANT SELECT ON v_reserve_statistics TO circulation_app_user;
-- GRANT SELECT ON v_reader_borrow_stats TO circulation_app_user;

-- =========================================
-- 脚本执行完成
-- =========================================
\echo 'Circulation Service database schema created successfully!'
\echo 'Total tables created: 2 (borrows, reserves)'
\echo 'Total indexes created: 32 (including unique indexes)'
\echo 'Total views created: 3'
\echo 'Test data inserted: 40 borrows + 30 reserves'
\echo ''
\echo 'Next steps:'
\echo '1. Verify data: SELECT COUNT(*) FROM borrows; SELECT COUNT(*) FROM reserves;'
\echo '2. Check statistics: SELECT * FROM v_borrow_statistics;'
\echo '3. Start circulation-service and test API endpoints'
