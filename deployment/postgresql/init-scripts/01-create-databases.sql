-- ============================================
-- GCRF Library Management System
-- PostgreSQL Database Initialization Script
-- ============================================
-- This script creates all required databases for the microservices
-- It runs automatically when the PostgreSQL container starts for the first time
-- ============================================

-- Create databases for each microservice
CREATE DATABASE gcrf_auth;
CREATE DATABASE gcrf_book;
CREATE DATABASE gcrf_circulation;
CREATE DATABASE gcrf_reader;
CREATE DATABASE gcrf_system;
CREATE DATABASE gcrf_notification;

-- Create a dedicated application user with limited privileges
-- Password will be set via environment variable
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_user
      WHERE  usename = 'gcrf_app') THEN

      CREATE USER gcrf_app WITH
        PASSWORD 'changeme'  -- This should be overridden by environment variable
        NOSUPERUSER
        CREATEDB
        NOCREATEROLE
        INHERIT
        LOGIN
        NOREPLICATION
        NOBYPASSRLS
        CONNECTION LIMIT 100;
   END IF;
END
$do$;

-- Grant privileges to the application user
GRANT CONNECT ON DATABASE gcrf_auth TO gcrf_app;
GRANT CONNECT ON DATABASE gcrf_book TO gcrf_app;
GRANT CONNECT ON DATABASE gcrf_circulation TO gcrf_app;
GRANT CONNECT ON DATABASE gcrf_reader TO gcrf_app;
GRANT CONNECT ON DATABASE gcrf_system TO gcrf_app;
GRANT CONNECT ON DATABASE gcrf_notification TO gcrf_app;

-- Connect to each database and grant schema privileges
\c gcrf_auth;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

\c gcrf_book;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

\c gcrf_circulation;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

\c gcrf_reader;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

\c gcrf_system;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

\c gcrf_notification;
GRANT ALL PRIVILEGES ON SCHEMA public TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO gcrf_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO gcrf_app;

-- Create extensions needed for the application
\c gcrf_auth;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c gcrf_book;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For full-text search
CREATE EXTENSION IF NOT EXISTS "btree_gin"; -- For compound indexes

\c gcrf_circulation;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c gcrf_reader;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c gcrf_system;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c gcrf_notification;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Success message
\echo 'All GCRF databases created successfully!'