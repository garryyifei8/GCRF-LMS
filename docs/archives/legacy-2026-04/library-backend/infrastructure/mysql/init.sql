-- ============================================
-- Library Management System - Database Initialization
-- Author: Li Si (Data Engineer)
-- Date: 2025-10-11
-- ============================================

-- Create databases with utf8mb4 charset
CREATE DATABASE IF NOT EXISTS library_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_book CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_circulation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_reader CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_recommend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_nlp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_file CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS library_search CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application user with proper privileges
CREATE USER IF NOT EXISTS 'library_app'@'%' IDENTIFIED BY 'library_app_2024';

-- Grant privileges to application user for all databases
GRANT ALL PRIVILEGES ON library_auth.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_book.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_circulation.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_reader.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_system.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_recommend.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_nlp.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_vision.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_analytics.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_notification.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_file.* TO 'library_app'@'%';
GRANT ALL PRIVILEGES ON library_search.* TO 'library_app'@'%';

-- Create replication user for master-slave setup
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl_password_2024';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Display created databases
SHOW DATABASES;
