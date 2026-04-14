-- =====================================================
-- V007: Create Inventory Management Tables
-- Author: GCRF Team
-- Date: 2025-12-20
-- Description: Create inventory, inventory_task, and inventory_task_item tables
-- =====================================================

-- Inventory table (库存表)
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    location VARCHAR(100),
    shelf_number VARCHAR(50),
    total_quantity INT DEFAULT 0,
    available_quantity INT DEFAULT 0,
    borrowed_quantity INT DEFAULT 0,
    reserved_quantity INT DEFAULT 0,
    alert_threshold INT DEFAULT 5,
    last_check_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0,
    CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Indexes for inventory table
CREATE INDEX idx_inventory_book_id ON inventory(book_id);
CREATE INDEX idx_inventory_location ON inventory(location);
CREATE INDEX idx_inventory_deleted ON inventory(deleted);
CREATE INDEX idx_inventory_alert ON inventory(available_quantity, alert_threshold) WHERE deleted = 0;

-- Comments for inventory table
COMMENT ON TABLE inventory IS '库存表';
COMMENT ON COLUMN inventory.id IS '库存ID';
COMMENT ON COLUMN inventory.book_id IS '图书ID';
COMMENT ON COLUMN inventory.location IS '存放位置';
COMMENT ON COLUMN inventory.shelf_number IS '书架号';
COMMENT ON COLUMN inventory.total_quantity IS '馆藏总数';
COMMENT ON COLUMN inventory.available_quantity IS '可借数量';
COMMENT ON COLUMN inventory.borrowed_quantity IS '已借出数量';
COMMENT ON COLUMN inventory.reserved_quantity IS '预约数量';
COMMENT ON COLUMN inventory.alert_threshold IS '预警阈值';
COMMENT ON COLUMN inventory.last_check_time IS '最后盘点时间';
COMMENT ON COLUMN inventory.created_at IS '创建时间';
COMMENT ON COLUMN inventory.updated_at IS '更新时间';
COMMENT ON COLUMN inventory.created_by IS '创建人ID';
COMMENT ON COLUMN inventory.updated_by IS '更新人ID';
COMMENT ON COLUMN inventory.deleted IS '删除标记：0-未删除，1-已删除';

-- Inventory Task table (盘点任务表)
CREATE TABLE inventory_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    task_code VARCHAR(50) UNIQUE,
    task_type VARCHAR(50) DEFAULT 'FULL',
    status VARCHAR(50) DEFAULT 'PENDING',
    scope VARCHAR(50) DEFAULT 'ALL',
    location VARCHAR(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    plan_start_time TIMESTAMP,
    plan_end_time TIMESTAMP,
    operator_id BIGINT,
    operator_name VARCHAR(100),
    total_books INT DEFAULT 0,
    checked_books INT DEFAULT 0,
    discrepancy_count INT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

-- Indexes for inventory_task table
CREATE INDEX idx_inventory_task_status ON inventory_task(status);
CREATE INDEX idx_inventory_task_code ON inventory_task(task_code);
CREATE INDEX idx_inventory_task_deleted ON inventory_task(deleted);
CREATE INDEX idx_inventory_task_operator ON inventory_task(operator_id);

-- Comments for inventory_task table
COMMENT ON TABLE inventory_task IS '盘点任务表';
COMMENT ON COLUMN inventory_task.id IS '任务ID';
COMMENT ON COLUMN inventory_task.task_name IS '任务名称';
COMMENT ON COLUMN inventory_task.task_code IS '任务编号（唯一）';
COMMENT ON COLUMN inventory_task.task_type IS '任务类型：FULL-全面盘点，PARTIAL-部分盘点，SPOT-抽查盘点';
COMMENT ON COLUMN inventory_task.status IS '任务状态：PENDING-待执行，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消';
COMMENT ON COLUMN inventory_task.scope IS '盘点范围：ALL-全部，LOCATION-按位置，CATEGORY-按分类';
COMMENT ON COLUMN inventory_task.location IS '盘点位置（当scope为LOCATION时使用）';
COMMENT ON COLUMN inventory_task.start_time IS '实际开始时间';
COMMENT ON COLUMN inventory_task.end_time IS '实际结束时间';
COMMENT ON COLUMN inventory_task.plan_start_time IS '计划开始时间';
COMMENT ON COLUMN inventory_task.plan_end_time IS '计划结束时间';
COMMENT ON COLUMN inventory_task.operator_id IS '操作人ID';
COMMENT ON COLUMN inventory_task.operator_name IS '操作人姓名';
COMMENT ON COLUMN inventory_task.total_books IS '总图书数';
COMMENT ON COLUMN inventory_task.checked_books IS '已盘点数';
COMMENT ON COLUMN inventory_task.discrepancy_count IS '差异数量';
COMMENT ON COLUMN inventory_task.notes IS '备注';
COMMENT ON COLUMN inventory_task.created_at IS '创建时间';
COMMENT ON COLUMN inventory_task.updated_at IS '更新时间';
COMMENT ON COLUMN inventory_task.created_by IS '创建人ID';
COMMENT ON COLUMN inventory_task.updated_by IS '更新人ID';
COMMENT ON COLUMN inventory_task.deleted IS '删除标记：0-未删除，1-已删除';

-- Inventory Task Item table (盘点明细表)
CREATE TABLE inventory_task_item (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    book_title VARCHAR(500),
    isbn VARCHAR(20),
    expected_quantity INT DEFAULT 0,
    actual_quantity INT,
    discrepancy INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    checked_time TIMESTAMP,
    checker_id BIGINT,
    checker_name VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_item_task FOREIGN KEY (task_id) REFERENCES inventory_task(id),
    CONSTRAINT fk_task_item_book FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Indexes for inventory_task_item table
CREATE INDEX idx_task_item_task_id ON inventory_task_item(task_id);
CREATE INDEX idx_task_item_book_id ON inventory_task_item(book_id);
CREATE INDEX idx_task_item_status ON inventory_task_item(status);

-- Comments for inventory_task_item table
COMMENT ON TABLE inventory_task_item IS '盘点明细表';
COMMENT ON COLUMN inventory_task_item.id IS '明细ID';
COMMENT ON COLUMN inventory_task_item.task_id IS '任务ID';
COMMENT ON COLUMN inventory_task_item.book_id IS '图书ID';
COMMENT ON COLUMN inventory_task_item.book_title IS '图书标题';
COMMENT ON COLUMN inventory_task_item.isbn IS 'ISBN号';
COMMENT ON COLUMN inventory_task_item.expected_quantity IS '期望数量';
COMMENT ON COLUMN inventory_task_item.actual_quantity IS '实际数量';
COMMENT ON COLUMN inventory_task_item.discrepancy IS '差异数量';
COMMENT ON COLUMN inventory_task_item.status IS '状态：PENDING-待盘点，CHECKED-已盘点，SKIPPED-已跳过';
COMMENT ON COLUMN inventory_task_item.checked_time IS '盘点时间';
COMMENT ON COLUMN inventory_task_item.checker_id IS '盘点人ID';
COMMENT ON COLUMN inventory_task_item.checker_name IS '盘点人姓名';
COMMENT ON COLUMN inventory_task_item.notes IS '备注';
COMMENT ON COLUMN inventory_task_item.created_at IS '创建时间';
COMMENT ON COLUMN inventory_task_item.updated_at IS '更新时间';

-- Create function to generate task code
CREATE OR REPLACE FUNCTION generate_inventory_task_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.task_code IS NULL OR NEW.task_code = '' THEN
        NEW.task_code := 'IT' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || LPAD(NEW.id::text, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for auto-generating task code
CREATE TRIGGER trg_inventory_task_code
    BEFORE INSERT ON inventory_task
    FOR EACH ROW
    EXECUTE FUNCTION generate_inventory_task_code();

-- Create function to update inventory_task.updated_at
CREATE OR REPLACE FUNCTION update_inventory_task_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for auto-updating timestamp
CREATE TRIGGER trg_inventory_task_updated_at
    BEFORE UPDATE ON inventory_task
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_task_timestamp();

-- Create trigger for auto-updating inventory timestamp
CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_task_timestamp();

-- Create trigger for auto-updating inventory_task_item timestamp
CREATE TRIGGER trg_inventory_task_item_updated_at
    BEFORE UPDATE ON inventory_task_item
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_task_timestamp();
