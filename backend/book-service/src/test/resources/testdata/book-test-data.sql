-- Test data for book-service integration tests

-- Clean existing test data
DELETE FROM book_category_mapping WHERE book_id >= 1000;
DELETE FROM books WHERE id >= 1000;
DELETE FROM book_category WHERE id >= 1000;

-- Insert test categories
INSERT INTO book_category (id, parent_id, category_name, category_code, path, level, description, status)
VALUES
    (1000, NULL, 'Computer Science', 'CS', '1000', 1, 'Computer Science and Technology', 'ACTIVE'),
    (1001, 1000, 'Programming', 'CS.PROG', '1000.1001', 2, 'Programming Languages and Development', 'ACTIVE'),
    (1002, 1000, 'Artificial Intelligence', 'CS.AI', '1000.1002', 2, 'AI and Machine Learning', 'ACTIVE'),
    (1003, NULL, 'Literature', 'LIT', '1003', 1, 'Literature and Fiction', 'ACTIVE'),
    (1004, 1003, 'Fiction', 'LIT.FIC', '1003.1004', 2, 'Fiction Books', 'ACTIVE'),
    (1005, NULL, 'Inactive Category', 'INACTIVE', '1005', 1, 'Inactive test category', 'INACTIVE');

-- Insert test books
INSERT INTO books (id, isbn, title, subtitle, author, translator, publisher, publish_date,
                   edition, pages, price, binding, language, classification_code, subject_keywords,
                   abstract, cover_url, pdf_url, pdf_file_name, pdf_file_size,
                   total_quantity, available_quantity, borrowed_quantity, reserved_quantity,
                   version, status, created_at, updated_at)
VALUES
    -- Book 1: Available book
    (1000, '9781234567890', 'Test Book 1', 'Introduction to Testing', 'John Doe', NULL,
     'Test Publisher', '2023-01-01', '1st Edition', 300, 49.99, 'Paperback', 'English',
     'CS.PROG', 'testing, integration, java', 'A comprehensive guide to integration testing',
     'http://example.com/cover1.jpg', NULL, NULL, NULL,
     10, 8, 2, 0, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Book 2: Out of stock book (total=5, available=0, borrowed=5, reserved=0 to satisfy consistency)
    (1001, '9781234567891', 'Test Book 2', 'Advanced Testing', 'Jane Smith', 'Tom Brown',
     'Tech Publisher', '2023-06-15', '2nd Edition', 450, 69.99, 'Hardcover', 'English',
     'CS.AI', 'testing, advanced, automation', 'Advanced testing techniques',
     'http://example.com/cover2.jpg', 'http://example.com/book2.pdf', 'book2.pdf', 1048576,
     5, 0, 5, 0, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Book 3: Inactive book
    (1002, '9781234567892', 'Test Book 3', 'Legacy Testing', 'Old Author', NULL,
     'Old Publisher', '2010-01-01', '1st Edition', 200, 29.99, 'Paperback', 'English',
     'CS.PROG', 'legacy, testing', 'Old testing methods',
     NULL, NULL, NULL, NULL,
     3, 3, 0, 0, 0, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Book 4: Book with PDF
    (1003, '9781234567893', 'Test Book 4', 'Digital Testing', 'Digital Author', NULL,
     'Digital Publisher', '2024-01-01', '1st Edition', 350, 59.99, 'eBook', 'English',
     'CS.AI', 'digital, testing, pdf', 'Digital testing resources',
     'http://example.com/cover4.jpg', 'http://example.com/book4.pdf', 'book4.pdf', 2097152,
     100, 95, 3, 2, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Book 5: Another available book for search tests
    (1004, '9781234567894', 'Integration Testing Guide', 'Complete Reference', 'Test Expert', NULL,
     'Quality Press', '2024-06-01', '3rd Edition', 500, 79.99, 'Hardcover', 'English',
     'CS.PROG', 'integration, testing, guide', 'Complete guide to integration testing',
     'http://example.com/cover5.jpg', NULL, NULL, NULL,
     15, 12, 3, 0, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Map books to categories
INSERT INTO book_category_mapping (book_id, category_id, is_primary)
VALUES
    (1000, 1001, true),   -- Book 1 -> Programming (primary)
    (1000, 1000, false),  -- Book 1 -> Computer Science (secondary)
    (1001, 1002, true),   -- Book 2 -> AI (primary)
    (1002, 1001, true),   -- Book 3 -> Programming (primary)
    (1003, 1002, true),   -- Book 4 -> AI (primary)
    (1004, 1001, true),   -- Book 5 -> Programming (primary)
    (1004, 1000, false);  -- Book 5 -> Computer Science (secondary)

-- Update category book counts
UPDATE book_category SET book_count = 3 WHERE id = 1001; -- Programming has 3 books
UPDATE book_category SET book_count = 2 WHERE id = 1002; -- AI has 2 books
UPDATE book_category SET book_count = 5 WHERE id = 1000; -- CS has 5 books (includes secondary)
UPDATE book_category SET child_count = 2 WHERE id = 1000; -- CS has 2 children
UPDATE book_category SET child_count = 1 WHERE id = 1003; -- Literature has 1 child

-- Reset sequence
SELECT setval('books_id_seq', 1004);
SELECT setval('book_category_id_seq', 1005);
