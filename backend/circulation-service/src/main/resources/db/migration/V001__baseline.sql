-- =========================================
-- V001: Baseline migration for circulation-service
-- Database: circulation_service
-- Description: Creates borrows and reserves tables with indexes
-- =========================================

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

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

-- 外键索引 - 高频查询字段
CREATE INDEX IF NOT EXISTS idx_borrows_reader_id ON borrows(reader_id);
CREATE INDEX IF NOT EXISTS idx_borrows_book_id ON borrows(book_id);
CREATE INDEX IF NOT EXISTS idx_borrows_book_barcode ON borrows(book_barcode) WHERE book_barcode IS NOT NULL;

-- 状态和日期索引
CREATE INDEX IF NOT EXISTS idx_borrows_status ON borrows(status);
CREATE INDEX IF NOT EXISTS idx_borrows_borrow_date ON borrows(borrow_date DESC);
CREATE INDEX IF NOT EXISTS idx_borrows_due_date ON borrows(due_date);
CREATE INDEX IF NOT EXISTS idx_borrows_return_date ON borrows(return_date) WHERE return_date IS NOT NULL;

-- 复合索引 - 常见组合查询
CREATE INDEX IF NOT EXISTS idx_borrows_reader_status ON borrows(reader_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_borrows_book_status ON borrows(book_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_borrows_status_due ON borrows(status, due_date) WHERE deleted_at IS NULL;

-- 部分索引 - 优化活跃借阅查询
CREATE INDEX IF NOT EXISTS idx_borrows_active ON borrows(reader_id, book_id)
    WHERE status = 'BORROWED' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_borrows_overdue ON borrows(reader_id, due_date)
    WHERE status = 'OVERDUE' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_borrows_unreturned ON borrows(reader_id, borrow_date DESC)
    WHERE return_date IS NULL AND deleted_at IS NULL;

-- 罚金相关索引
CREATE INDEX IF NOT EXISTS idx_borrows_unpaid_fine ON borrows(reader_id, fine_amount)
    WHERE fine_amount > 0 AND fine_paid = FALSE AND deleted_at IS NULL;

-- =========================================
-- 4. 创建索引 - reserves表
-- =========================================

-- 外键索引
CREATE INDEX IF NOT EXISTS idx_reserves_reader_id ON reserves(reader_id);
CREATE INDEX IF NOT EXISTS idx_reserves_book_id ON reserves(book_id);

-- 状态和日期索引
CREATE INDEX IF NOT EXISTS idx_reserves_status ON reserves(status);
CREATE INDEX IF NOT EXISTS idx_reserves_reserve_date ON reserves(reserve_date DESC);
CREATE INDEX IF NOT EXISTS idx_reserves_expiry_date ON reserves(expiry_date);

-- 复合索引
CREATE INDEX IF NOT EXISTS idx_reserves_reader_status ON reserves(reader_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reserves_book_status ON reserves(book_id, status) WHERE deleted_at IS NULL;

-- 部分索引 - 优化活跃预约查询
CREATE INDEX IF NOT EXISTS idx_reserves_active ON reserves(book_id, reserve_date)
    WHERE status = 'RESERVED' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reserves_pending_pickup ON reserves(reader_id, expiry_date)
    WHERE status = 'RESERVED' AND pickup_date IS NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reserves_pending_notify ON reserves(reader_id)
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
DROP TRIGGER IF EXISTS trigger_borrows_updated_at ON borrows;
CREATE TRIGGER trigger_borrows_updated_at
    BEFORE UPDATE ON borrows
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为reserves表添加触发器
DROP TRIGGER IF EXISTS trigger_reserves_updated_at ON reserves;
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
