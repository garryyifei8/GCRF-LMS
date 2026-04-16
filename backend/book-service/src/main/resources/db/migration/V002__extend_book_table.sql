-- =========================================
-- V002: Extend Book Table for Inventory Tracking
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- =========================================
-- 1. Add Missing Book Metadata Columns
-- =========================================

ALTER TABLE books ADD COLUMN IF NOT EXISTS subtitle VARCHAR(500);
ALTER TABLE books ADD COLUMN IF NOT EXISTS translator VARCHAR(200);
ALTER TABLE books ADD COLUMN IF NOT EXISTS edition VARCHAR(100);
ALTER TABLE books ADD COLUMN IF NOT EXISTS binding VARCHAR(50);

-- Rename description to abstract (Entity maps to "abstract" column)
ALTER TABLE books RENAME COLUMN description TO abstract;

-- Add classification and keyword columns
ALTER TABLE books ADD COLUMN IF NOT EXISTS classification_code VARCHAR(50);
ALTER TABLE books ADD COLUMN IF NOT EXISTS subject_keywords TEXT;

-- =========================================
-- 2. Add Inventory Tracking Columns to Books Table
-- =========================================

-- Add "borrowed" and "reserved" quantity columns using the Entity's naming convention
ALTER TABLE books ADD COLUMN IF NOT EXISTS borrowed_quantity INTEGER DEFAULT 0
    CONSTRAINT chk_borrowed_quantity CHECK (borrowed_quantity >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER DEFAULT 0
    CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity >= 0);

-- Add version column for optimistic locking
ALTER TABLE books ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Add barcode column
ALTER TABLE books ADD COLUMN IF NOT EXISTS barcode VARCHAR(50);

-- Add alias columns matching the "_copies" naming used in V003/V004 functions/views.
-- These are kept in sync with the primary "_quantity" columns via a trigger below.
ALTER TABLE books ADD COLUMN IF NOT EXISTS total_copies INTEGER DEFAULT 1
    CONSTRAINT chk_total_copies CHECK (total_copies >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS available_copies INTEGER DEFAULT 1
    CONSTRAINT chk_available_copies CHECK (available_copies >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS borrowed_copies INTEGER DEFAULT 0
    CONSTRAINT chk_borrowed_copies CHECK (borrowed_copies >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS reserved_copies INTEGER DEFAULT 0
    CONSTRAINT chk_reserved_copies CHECK (reserved_copies >= 0);

-- Sync _copies columns from _quantity columns for any pre-existing rows
UPDATE books
SET total_copies     = COALESCE(total_quantity, 1),
    available_copies = COALESCE(available_quantity, 1),
    borrowed_copies  = COALESCE(borrowed_quantity, 0),
    reserved_copies  = COALESCE(reserved_quantity, 0);

-- Keep _copies in sync with _quantity on every write
CREATE OR REPLACE FUNCTION sync_book_inventory_copies()
RETURNS TRIGGER AS $$
BEGIN
    NEW.total_copies     := NEW.total_quantity;
    NEW.available_copies := NEW.available_quantity;
    NEW.borrowed_copies  := NEW.borrowed_quantity;
    NEW.reserved_copies  := NEW.reserved_quantity;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_book_inventory_copies
    BEFORE INSERT OR UPDATE OF total_quantity, available_quantity,
                               borrowed_quantity, reserved_quantity
    ON books
    FOR EACH ROW
    EXECUTE FUNCTION sync_book_inventory_copies();

-- =========================================
-- 3. Create Inventory Transaction Log Table
-- =========================================
CREATE TABLE IF NOT EXISTS book_inventory_log (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    quantity_change INTEGER NOT NULL,
    total_before INTEGER NOT NULL,
    total_after INTEGER NOT NULL,
    available_before INTEGER NOT NULL,
    available_after INTEGER NOT NULL,
    borrowed_before INTEGER NOT NULL,
    borrowed_after INTEGER NOT NULL,
    reserved_before INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    reason VARCHAR(500),
    operator_id BIGINT,
    operator_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_inventory_log_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN (
        'INITIAL_STOCK',    -- 初始入库
        'PURCHASE',         -- 采购入库
        'DONATION',         -- 捐赠入库
        'BORROW',           -- 借出
        'RETURN',           -- 归还
        'RESERVE',          -- 预约
        'CANCEL_RESERVE',   -- 取消预约
        'FULFILL_RESERVE',  -- 预约转借阅
        'LOSS',             -- 丢失
        'DAMAGE',           -- 损坏
        'DISCARD',          -- 报废
        'ADJUSTMENT'        -- 人工调整
    ))
);

-- =========================================
-- 4. Create Inventory Snapshot Table (for daily statistics)
-- =========================================
CREATE TABLE IF NOT EXISTS book_inventory_snapshot (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_copies INTEGER NOT NULL,
    available_copies INTEGER NOT NULL,
    borrowed_copies INTEGER NOT NULL,
    reserved_copies INTEGER NOT NULL,
    borrow_count INTEGER DEFAULT 0,
    return_count INTEGER DEFAULT 0,
    reserve_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_snapshot_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE CASCADE,

    CONSTRAINT uk_book_snapshot_date UNIQUE (book_id, snapshot_date)
);

-- =========================================
-- 5. Create Indexes for Performance
-- =========================================

CREATE INDEX IF NOT EXISTS idx_books_available_quantity ON books(available_quantity)
    WHERE deleted_at IS NULL AND available_quantity > 0;

CREATE INDEX IF NOT EXISTS idx_books_borrowed_quantity ON books(borrowed_quantity)
    WHERE deleted_at IS NULL AND borrowed_quantity > 0;

CREATE INDEX IF NOT EXISTS idx_books_version ON books(version);

CREATE INDEX IF NOT EXISTS idx_inventory_log_book_id ON book_inventory_log(book_id);
CREATE INDEX IF NOT EXISTS idx_inventory_log_type ON book_inventory_log(transaction_type);
CREATE INDEX IF NOT EXISTS idx_inventory_log_created ON book_inventory_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_log_operator ON book_inventory_log(operator_id);

CREATE INDEX IF NOT EXISTS idx_snapshot_book_id ON book_inventory_snapshot(book_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_date ON book_inventory_snapshot(snapshot_date DESC);
CREATE INDEX IF NOT EXISTS idx_snapshot_book_date ON book_inventory_snapshot(book_id, snapshot_date DESC);

-- =========================================
-- 6. Functions for Inventory Management
-- =========================================

-- Function to create daily inventory snapshot
CREATE OR REPLACE FUNCTION create_daily_inventory_snapshot(p_date DATE DEFAULT CURRENT_DATE)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    INSERT INTO book_inventory_snapshot (
        book_id, snapshot_date,
        total_copies, available_copies, borrowed_copies, reserved_copies,
        borrow_count, return_count, reserve_count
    )
    SELECT
        b.id,
        p_date,
        b.total_copies,
        b.available_copies,
        b.borrowed_copies,
        b.reserved_copies,
        COALESCE(borrow_stats.cnt, 0),
        COALESCE(return_stats.cnt, 0),
        COALESCE(reserve_stats.cnt, 0)
    FROM books b
    LEFT JOIN (
        SELECT book_id, COUNT(*) AS cnt
        FROM book_inventory_log
        WHERE transaction_type = 'BORROW' AND DATE(created_at) = p_date
        GROUP BY book_id
    ) borrow_stats ON b.id = borrow_stats.book_id
    LEFT JOIN (
        SELECT book_id, COUNT(*) AS cnt
        FROM book_inventory_log
        WHERE transaction_type = 'RETURN' AND DATE(created_at) = p_date
        GROUP BY book_id
    ) return_stats ON b.id = return_stats.book_id
    LEFT JOIN (
        SELECT book_id, COUNT(*) AS cnt
        FROM book_inventory_log
        WHERE transaction_type = 'RESERVE' AND DATE(created_at) = p_date
        GROUP BY book_id
    ) reserve_stats ON b.id = reserve_stats.book_id
    WHERE b.deleted_at IS NULL
    ON CONFLICT (book_id, snapshot_date)
    DO UPDATE SET
        total_copies     = EXCLUDED.total_copies,
        available_copies = EXCLUDED.available_copies,
        borrowed_copies  = EXCLUDED.borrowed_copies,
        reserved_copies  = EXCLUDED.reserved_copies,
        borrow_count     = EXCLUDED.borrow_count,
        return_count     = EXCLUDED.return_count,
        reserve_count    = EXCLUDED.reserve_count,
        created_at       = CURRENT_TIMESTAMP;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- Comments
-- =========================================
COMMENT ON COLUMN books.subtitle IS '副标题';
COMMENT ON COLUMN books.translator IS '译者';
COMMENT ON COLUMN books.edition IS '版次';
COMMENT ON COLUMN books.binding IS '装帧方式';
COMMENT ON COLUMN books.abstract IS '摘要/简介';
COMMENT ON COLUMN books.classification_code IS '分类代码';
COMMENT ON COLUMN books.subject_keywords IS '主题关键词';
COMMENT ON COLUMN books.total_quantity IS '图书总库存数量';
COMMENT ON COLUMN books.available_quantity IS '可借阅数量';
COMMENT ON COLUMN books.borrowed_quantity IS '已借出数量';
COMMENT ON COLUMN books.reserved_quantity IS '已预约数量';
COMMENT ON COLUMN books.version IS '乐观锁版本号';
COMMENT ON COLUMN books.barcode IS '条形码';

COMMENT ON TABLE book_inventory_log IS '图书库存变动日志表';
COMMENT ON TABLE book_inventory_snapshot IS '图书库存每日快照表';

-- =========================================
-- Migration completed successfully
-- =========================================
