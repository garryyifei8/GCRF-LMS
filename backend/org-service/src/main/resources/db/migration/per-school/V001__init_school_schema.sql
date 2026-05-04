-- Initial per-school schema scaffolding.
-- Each school's schema gets its own copy of these tables.
-- Will be extended by other services (book-service, circulation-service, ...)
-- in their own per-school migrations as features come online.

CREATE TABLE IF NOT EXISTS school_meta (
    school_code         VARCHAR(50)  PRIMARY KEY,
    school_name         VARCHAR(200) NOT NULL,
    initialized_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    metadata            JSONB        NOT NULL DEFAULT '{}'
);

-- Reader (will be expanded by reader-service migrations)
CREATE TABLE IF NOT EXISTS reader (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    card_number     VARCHAR(30)  UNIQUE,
    grade           VARCHAR(20),
    class           VARCHAR(20),
    max_borrow      INT          NOT NULL DEFAULT 5,
    borrow_days     INT          NOT NULL DEFAULT 30,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_reader_user_id ON reader(user_id);

-- Book catalog skeleton (will be expanded by book-service migrations)
CREATE TABLE IF NOT EXISTS book_catalog (
    id              BIGSERIAL    PRIMARY KEY,
    isbn            VARCHAR(20),
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(500),
    classification  VARCHAR(50),
    total_count     INT          NOT NULL DEFAULT 0,
    available_count INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_book_catalog_isbn ON book_catalog(isbn);

-- Book copy (will be expanded)
CREATE TABLE IF NOT EXISTS book_copy (
    id              BIGSERIAL    PRIMARY KEY,
    catalog_id      BIGINT       NOT NULL REFERENCES book_catalog(id),
    barcode         VARCHAR(50)  UNIQUE NOT NULL,
    call_no         VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'IN',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Borrow record skeleton (will be expanded)
CREATE TABLE IF NOT EXISTS borrow_record (
    id                BIGSERIAL    PRIMARY KEY,
    reader_id         BIGINT       NOT NULL,
    copy_id           BIGINT       NOT NULL,
    borrow_at         TIMESTAMPTZ  NOT NULL,
    due_at            TIMESTAMPTZ  NOT NULL,
    return_at         TIMESTAMPTZ,
    idempotency_key   VARCHAR(120) UNIQUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_borrow_active
    ON borrow_record(reader_id, return_at) WHERE return_at IS NULL;
