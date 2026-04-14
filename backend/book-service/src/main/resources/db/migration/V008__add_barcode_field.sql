-- Add barcode field to books table
-- V008: Add barcode field for book identification

-- Add barcode column
ALTER TABLE books ADD COLUMN IF NOT EXISTS barcode VARCHAR(50);

-- Create unique index on barcode
CREATE UNIQUE INDEX IF NOT EXISTS idx_books_barcode ON books(barcode) WHERE barcode IS NOT NULL;

-- Create sequence for barcode generation
CREATE SEQUENCE IF NOT EXISTS book_barcode_seq START WITH 1 INCREMENT BY 1;

-- Add comment
COMMENT ON COLUMN books.barcode IS 'Unique barcode for book identification';
