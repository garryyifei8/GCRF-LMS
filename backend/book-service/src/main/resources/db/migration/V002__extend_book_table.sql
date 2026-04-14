-- =========================================
-- V002: Extend Book Table for Inventory Tracking
-- Database: gcrf_book
-- Author: Database Architect
-- Date: 2025-11-04
-- =========================================

-- =========================================
-- 1. Add Inventory Tracking Columns to Books Table
-- =========================================

-- Add inventory columns
ALTER TABLE books ADD COLUMN IF NOT EXISTS total_copies INTEGER DEFAULT 1
    CONSTRAINT chk_total_copies CHECK (total_copies >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS borrowed_copies INTEGER DEFAULT 0
    CONSTRAINT chk_borrowed_copies CHECK (borrowed_copies >= 0);

ALTER TABLE books ADD COLUMN IF NOT EXISTS reserved_copies INTEGER DEFAULT 0
    CONSTRAINT chk_reserved_copies CHECK (reserved_copies >= 0);

-- Rename existing column for consistency
ALTER TABLE books RENAME COLUMN available_quantity TO available_copies;

-- Add version column for optimistic locking
ALTER TABLE books ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Add constraint to ensure inventory consistency
ALTER TABLE books ADD CONSTRAINT chk_inventory_consistency
    CHECK (total_copies = available_copies + borrowed_copies + reserved_copies);

-- =========================================
-- 2. Create Inventory Transaction Log Table
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
-- 3. Create Inventory Snapshot Table (for daily statistics)
-- =========================================
CREATE TABLE IF NOT EXISTS book_inventory_snapshot (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_copies INTEGER NOT NULL,
    available_copies INTEGER NOT NULL,
    borrowed_copies INTEGER NOT NULL,
    reserved_copies INTEGER NOT NULL,
    borrow_count INTEGER DEFAULT 0,     -- 当日借出次数
    return_count INTEGER DEFAULT 0,     -- 当日归还次数
    reserve_count INTEGER DEFAULT 0,    -- 当日预约次数
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_snapshot_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE CASCADE,

    -- Unique constraint
    CONSTRAINT uk_book_snapshot_date UNIQUE (book_id, snapshot_date)
);

-- =========================================
-- 4. Create Indexes for Performance
-- =========================================

-- Inventory tracking indexes
CREATE INDEX idx_books_available_copies ON books(available_copies)
    WHERE deleted_at IS NULL AND available_copies > 0;

CREATE INDEX idx_books_borrowed_copies ON books(borrowed_copies)
    WHERE deleted_at IS NULL AND borrowed_copies > 0;

CREATE INDEX idx_books_reserved_copies ON books(reserved_copies)
    WHERE deleted_at IS NULL AND reserved_copies > 0;

CREATE INDEX idx_books_version ON books(version);

-- Inventory log indexes
CREATE INDEX idx_inventory_log_book_id ON book_inventory_log(book_id);
CREATE INDEX idx_inventory_log_type ON book_inventory_log(transaction_type);
CREATE INDEX idx_inventory_log_created ON book_inventory_log(created_at DESC);
CREATE INDEX idx_inventory_log_operator ON book_inventory_log(operator_id);

-- Snapshot indexes
CREATE INDEX idx_snapshot_book_id ON book_inventory_snapshot(book_id);
CREATE INDEX idx_snapshot_date ON book_inventory_snapshot(snapshot_date DESC);
CREATE INDEX idx_snapshot_book_date ON book_inventory_snapshot(book_id, snapshot_date DESC);

-- =========================================
-- 5. Functions for Inventory Management
-- =========================================

-- Function to safely update inventory with optimistic locking
CREATE OR REPLACE FUNCTION update_book_inventory(
    p_book_id BIGINT,
    p_available_delta INTEGER,
    p_borrowed_delta INTEGER,
    p_reserved_delta INTEGER,
    p_total_delta INTEGER,
    p_version BIGINT,
    p_transaction_type VARCHAR,
    p_reason VARCHAR,
    p_operator_id BIGINT,
    p_operator_name VARCHAR
) RETURNS TABLE (
    success BOOLEAN,
    message TEXT,
    new_version BIGINT,
    new_available INTEGER,
    new_borrowed INTEGER,
    new_reserved INTEGER,
    new_total INTEGER
) AS $$
DECLARE
    v_current_version BIGINT;
    v_current_available INTEGER;
    v_current_borrowed INTEGER;
    v_current_reserved INTEGER;
    v_current_total INTEGER;
    v_new_available INTEGER;
    v_new_borrowed INTEGER;
    v_new_reserved INTEGER;
    v_new_total INTEGER;
BEGIN
    -- Lock the row for update
    SELECT version, available_copies, borrowed_copies, reserved_copies, total_copies
    INTO v_current_version, v_current_available, v_current_borrowed, v_current_reserved, v_current_total
    FROM books
    WHERE id = p_book_id AND deleted_at IS NULL
    FOR UPDATE NOWAIT;

    -- Check if book exists
    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'Book not found'::TEXT, 0::BIGINT, 0, 0, 0, 0;
        RETURN;
    END IF;

    -- Check version for optimistic locking
    IF v_current_version != p_version THEN
        RETURN QUERY SELECT FALSE, 'Version mismatch - data has been modified'::TEXT,
            v_current_version, v_current_available, v_current_borrowed, v_current_reserved, v_current_total;
        RETURN;
    END IF;

    -- Calculate new values
    v_new_available := v_current_available + p_available_delta;
    v_new_borrowed := v_current_borrowed + p_borrowed_delta;
    v_new_reserved := v_current_reserved + p_reserved_delta;
    v_new_total := v_current_total + p_total_delta;

    -- Validate new values
    IF v_new_available < 0 THEN
        RETURN QUERY SELECT FALSE, 'Insufficient available copies'::TEXT,
            v_current_version, v_current_available, v_current_borrowed, v_current_reserved, v_current_total;
        RETURN;
    END IF;

    IF v_new_borrowed < 0 OR v_new_reserved < 0 OR v_new_total < 0 THEN
        RETURN QUERY SELECT FALSE, 'Invalid inventory values'::TEXT,
            v_current_version, v_current_available, v_current_borrowed, v_current_reserved, v_current_total;
        RETURN;
    END IF;

    -- Check inventory consistency
    IF v_new_total != v_new_available + v_new_borrowed + v_new_reserved THEN
        RETURN QUERY SELECT FALSE, 'Inventory consistency check failed'::TEXT,
            v_current_version, v_current_available, v_current_borrowed, v_current_reserved, v_current_total;
        RETURN;
    END IF;

    -- Update the book inventory
    UPDATE books
    SET available_copies = v_new_available,
        borrowed_copies = v_new_borrowed,
        reserved_copies = v_new_reserved,
        total_copies = v_new_total,
        version = version + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_book_id;

    -- Log the transaction
    INSERT INTO book_inventory_log (
        book_id, transaction_type, quantity_change,
        total_before, total_after,
        available_before, available_after,
        borrowed_before, borrowed_after,
        reserved_before, reserved_after,
        reason, operator_id, operator_name
    ) VALUES (
        p_book_id, p_transaction_type, p_total_delta,
        v_current_total, v_new_total,
        v_current_available, v_new_available,
        v_current_borrowed, v_new_borrowed,
        v_current_reserved, v_new_reserved,
        p_reason, p_operator_id, p_operator_name
    );

    RETURN QUERY SELECT TRUE, 'Inventory updated successfully'::TEXT,
        v_current_version + 1, v_new_available, v_new_borrowed, v_new_reserved, v_new_total;
END;
$$ LANGUAGE plpgsql;

-- Function to get inventory statistics
CREATE OR REPLACE FUNCTION get_inventory_statistics(
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
) RETURNS TABLE (
    total_books BIGINT,
    total_copies BIGINT,
    total_available BIGINT,
    total_borrowed BIGINT,
    total_reserved BIGINT,
    utilization_rate NUMERIC(5,2),
    books_with_zero_stock BIGINT,
    books_low_stock BIGINT  -- Less than 2 available
) AS $$
BEGIN
    -- Default date range if not provided
    IF p_start_date IS NULL THEN
        p_start_date := CURRENT_DATE - INTERVAL '30 days';
    END IF;
    IF p_end_date IS NULL THEN
        p_end_date := CURRENT_DATE;
    END IF;

    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_books,
        SUM(total_copies)::BIGINT AS total_copies,
        SUM(available_copies)::BIGINT AS total_available,
        SUM(borrowed_copies)::BIGINT AS total_borrowed,
        SUM(reserved_copies)::BIGINT AS total_reserved,
        CASE
            WHEN SUM(total_copies) > 0 THEN
                ROUND((SUM(borrowed_copies)::NUMERIC / SUM(total_copies)::NUMERIC) * 100, 2)
            ELSE 0
        END AS utilization_rate,
        COUNT(*) FILTER (WHERE available_copies = 0)::BIGINT AS books_with_zero_stock,
        COUNT(*) FILTER (WHERE available_copies > 0 AND available_copies < 2)::BIGINT AS books_low_stock
    FROM books
    WHERE deleted_at IS NULL
        AND status = 'ACTIVE';
END;
$$ LANGUAGE plpgsql;

-- Function to create daily inventory snapshot
CREATE OR REPLACE FUNCTION create_daily_inventory_snapshot(p_date DATE DEFAULT CURRENT_DATE)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Insert or update snapshot for each book
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
        COALESCE(borrow_stats.count, 0),
        COALESCE(return_stats.count, 0),
        COALESCE(reserve_stats.count, 0)
    FROM books b
    LEFT JOIN (
        SELECT book_id, COUNT(*) as count
        FROM book_inventory_log
        WHERE transaction_type = 'BORROW'
            AND DATE(created_at) = p_date
        GROUP BY book_id
    ) borrow_stats ON b.id = borrow_stats.book_id
    LEFT JOIN (
        SELECT book_id, COUNT(*) as count
        FROM book_inventory_log
        WHERE transaction_type = 'RETURN'
            AND DATE(created_at) = p_date
        GROUP BY book_id
    ) return_stats ON b.id = return_stats.book_id
    LEFT JOIN (
        SELECT book_id, COUNT(*) as count
        FROM book_inventory_log
        WHERE transaction_type = 'RESERVE'
            AND DATE(created_at) = p_date
        GROUP BY book_id
    ) reserve_stats ON b.id = reserve_stats.book_id
    WHERE b.deleted_at IS NULL
    ON CONFLICT (book_id, snapshot_date)
    DO UPDATE SET
        total_copies = EXCLUDED.total_copies,
        available_copies = EXCLUDED.available_copies,
        borrowed_copies = EXCLUDED.borrowed_copies,
        reserved_copies = EXCLUDED.reserved_copies,
        borrow_count = EXCLUDED.borrow_count,
        return_count = EXCLUDED.return_count,
        reserve_count = EXCLUDED.reserve_count,
        created_at = CURRENT_TIMESTAMP;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 6. Triggers
-- =========================================

-- Trigger to validate inventory changes
CREATE OR REPLACE FUNCTION validate_inventory_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure consistency
    IF NEW.total_copies != NEW.available_copies + NEW.borrowed_copies + NEW.reserved_copies THEN
        RAISE EXCEPTION 'Inventory consistency violation: total (%) != available (%) + borrowed (%) + reserved (%)',
            NEW.total_copies, NEW.available_copies, NEW.borrowed_copies, NEW.reserved_copies;
    END IF;

    -- Ensure non-negative values
    IF NEW.total_copies < 0 OR NEW.available_copies < 0 OR
       NEW.borrowed_copies < 0 OR NEW.reserved_copies < 0 THEN
        RAISE EXCEPTION 'Inventory values cannot be negative';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_inventory
    BEFORE INSERT OR UPDATE OF total_copies, available_copies, borrowed_copies, reserved_copies
    ON books
    FOR EACH ROW
    EXECUTE FUNCTION validate_inventory_change();

-- =========================================
-- 7. Views for Reporting
-- =========================================

-- View for books with low inventory
CREATE OR REPLACE VIEW v_low_inventory_books AS
SELECT
    b.id,
    b.isbn,
    b.title,
    b.author,
    b.total_copies,
    b.available_copies,
    b.borrowed_copies,
    b.reserved_copies,
    ROUND((b.borrowed_copies::NUMERIC / NULLIF(b.total_copies, 0)) * 100, 2) AS utilization_rate,
    CASE
        WHEN b.available_copies = 0 THEN 'OUT_OF_STOCK'
        WHEN b.available_copies < 2 THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM books b
WHERE b.deleted_at IS NULL
    AND b.status = 'ACTIVE'
    AND b.available_copies < 2
ORDER BY b.available_copies ASC, b.title;

-- View for inventory trends
CREATE OR REPLACE VIEW v_inventory_trends AS
SELECT
    snapshot_date,
    COUNT(DISTINCT book_id) AS total_books,
    SUM(total_copies) AS total_copies,
    SUM(available_copies) AS total_available,
    SUM(borrowed_copies) AS total_borrowed,
    SUM(reserved_copies) AS total_reserved,
    SUM(borrow_count) AS daily_borrows,
    SUM(return_count) AS daily_returns,
    SUM(reserve_count) AS daily_reserves,
    ROUND(AVG(
        CASE WHEN total_copies > 0
        THEN (borrowed_copies::NUMERIC / total_copies) * 100
        ELSE 0 END
    ), 2) AS avg_utilization_rate
FROM book_inventory_snapshot
GROUP BY snapshot_date
ORDER BY snapshot_date DESC;

-- =========================================
-- 8. Migrate Existing Data
-- =========================================

-- Initialize new columns with existing data
UPDATE books
SET total_copies = total_quantity,
    borrowed_copies = total_quantity - available_copies,
    reserved_copies = 0,
    version = 0
WHERE total_copies IS NULL;

-- Drop old columns (commented out for safety - run manually after verification)
-- ALTER TABLE books DROP COLUMN total_quantity;

-- Create initial inventory log entries
INSERT INTO book_inventory_log (
    book_id, transaction_type, quantity_change,
    total_before, total_after,
    available_before, available_after,
    borrowed_before, borrowed_after,
    reserved_before, reserved_after,
    reason
)
SELECT
    id,
    'INITIAL_STOCK',
    total_copies,
    0, total_copies,
    0, available_copies,
    0, borrowed_copies,
    0, reserved_copies,
    'Initial inventory migration'
FROM books
WHERE deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- =========================================
-- 9. Comments
-- =========================================
COMMENT ON COLUMN books.total_copies IS '图书总库存数量';
COMMENT ON COLUMN books.available_copies IS '可借阅数量';
COMMENT ON COLUMN books.borrowed_copies IS '已借出数量';
COMMENT ON COLUMN books.reserved_copies IS '已预约数量';
COMMENT ON COLUMN books.version IS '乐观锁版本号';

COMMENT ON TABLE book_inventory_log IS '图书库存变动日志表';
COMMENT ON COLUMN book_inventory_log.transaction_type IS '事务类型';
COMMENT ON COLUMN book_inventory_log.quantity_change IS '数量变化（正数为增加，负数为减少）';

COMMENT ON TABLE book_inventory_snapshot IS '图书库存每日快照表';
COMMENT ON COLUMN book_inventory_snapshot.snapshot_date IS '快照日期';
COMMENT ON COLUMN book_inventory_snapshot.borrow_count IS '当日借出次数';
COMMENT ON COLUMN book_inventory_snapshot.return_count IS '当日归还次数';
COMMENT ON COLUMN book_inventory_snapshot.reserve_count IS '当日预约次数';

COMMENT ON FUNCTION update_book_inventory IS '安全更新图书库存（支持乐观锁）';
COMMENT ON FUNCTION get_inventory_statistics IS '获取库存统计信息';
COMMENT ON FUNCTION create_daily_inventory_snapshot IS '创建每日库存快照';

-- =========================================
-- Migration completed successfully
-- Modified: 1 table (books)
-- New Tables: 2 (book_inventory_log, book_inventory_snapshot)
-- New Columns: 5 (total_copies, borrowed_copies, reserved_copies, version)
-- Indexes: 11
-- Functions: 3
-- Triggers: 1
-- Views: 2
-- =========================================