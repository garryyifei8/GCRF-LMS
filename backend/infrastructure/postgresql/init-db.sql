-- ====================================================================
-- 国创睿峰智能图书馆管理系统 - 数据库初始化脚本
-- 创建时间: 2025-10-11
-- 版本: v1.0
-- 描述: 创建12个微服务数据库、用户及权限配置
-- ====================================================================

-- 设置客户端编码
SET client_encoding = 'UTF8';

-- ====================================================================
-- 1. 创建扩展（在postgres数据库中）
-- ====================================================================
\c postgres

-- UUID生成扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 模糊搜索扩展
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 全文搜索扩展
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- PostGIS扩展（如需要地理位置功能）
-- CREATE EXTENSION IF NOT EXISTS "postgis";

-- 性能监控扩展
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- ====================================================================
-- 2. 创建角色和用户
-- ====================================================================

-- 创建只读角色
CREATE ROLE readonly_role;
GRANT CONNECT ON DATABASE postgres TO readonly_role;

-- 创建读写角色
CREATE ROLE readwrite_role;
GRANT CONNECT ON DATABASE postgres TO readwrite_role;

-- 创建管理员角色
CREATE ROLE admin_role;
GRANT CONNECT ON DATABASE postgres TO admin_role;

-- ====================================================================
-- 3. 创建微服务数据库和用户
-- ====================================================================

-- 3.1 认证服务数据库
CREATE DATABASE auth_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER auth_user WITH ENCRYPTED PASSWORD 'auth_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE auth_service TO auth_user;

\c auth_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO auth_user;

-- 3.2 图书服务数据库
\c postgres
CREATE DATABASE book_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER book_user WITH ENCRYPTED PASSWORD 'book_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE book_service TO book_user;

\c book_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
GRANT ALL ON SCHEMA public TO book_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO book_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO book_user;

-- 3.3 读者服务数据库
\c postgres
CREATE DATABASE reader_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER reader_user WITH ENCRYPTED PASSWORD 'reader_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE reader_service TO reader_user;

\c reader_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO reader_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO reader_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO reader_user;

-- 3.4 流通服务数据库
\c postgres
CREATE DATABASE circulation_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER circulation_user WITH ENCRYPTED PASSWORD 'circulation_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE circulation_service TO circulation_user;

\c circulation_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO circulation_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO circulation_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO circulation_user;

-- 3.5 系统管理服务数据库
\c postgres
CREATE DATABASE system_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER system_user WITH ENCRYPTED PASSWORD 'system_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE system_service TO system_user;

\c system_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO system_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO system_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO system_user;

-- 3.6 推荐服务数据库
\c postgres
CREATE DATABASE recommend_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER recommend_user WITH ENCRYPTED PASSWORD 'recommend_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE recommend_service TO recommend_user;

\c recommend_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO recommend_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO recommend_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO recommend_user;

-- 3.7 NLP服务数据库
\c postgres
CREATE DATABASE nlp_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER nlp_user WITH ENCRYPTED PASSWORD 'nlp_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE nlp_service TO nlp_user;

\c nlp_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO nlp_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO nlp_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO nlp_user;

-- 3.8 视觉服务数据库
\c postgres
CREATE DATABASE vision_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER vision_user WITH ENCRYPTED PASSWORD 'vision_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE vision_service TO vision_user;

\c vision_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO vision_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO vision_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO vision_user;

-- 3.9 分析服务数据库
\c postgres
CREATE DATABASE analytics_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER analytics_user WITH ENCRYPTED PASSWORD 'analytics_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE analytics_service TO analytics_user;

\c analytics_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
GRANT ALL ON SCHEMA public TO analytics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO analytics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO analytics_user;

-- 3.10 通知服务数据库
\c postgres
CREATE DATABASE notification_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER notification_user WITH ENCRYPTED PASSWORD 'notification_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE notification_service TO notification_user;

\c notification_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO notification_user;

-- 3.11 文件服务数据库
\c postgres
CREATE DATABASE file_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER file_user WITH ENCRYPTED PASSWORD 'file_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE file_service TO file_user;

\c file_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
GRANT ALL ON SCHEMA public TO file_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO file_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO file_user;

-- 3.12 搜索服务数据库
\c postgres
CREATE DATABASE search_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

CREATE USER search_user WITH ENCRYPTED PASSWORD 'search_pass_2024';
GRANT ALL PRIVILEGES ON DATABASE search_service TO search_user;

\c search_service
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "btree_gist";
GRANT ALL ON SCHEMA public TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO search_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO search_user;

-- ====================================================================
-- 4. 配置pg_stat_statements扩展（性能监控）
-- ====================================================================
\c postgres
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET pg_stat_statements.max = 10000;

-- ====================================================================
-- 5. 创建监控视图
-- ====================================================================
\c postgres

-- 创建数据库连接监控视图
CREATE OR REPLACE VIEW db_connections AS
SELECT
    datname as database,
    count(*) as connections,
    max(state) as state
FROM pg_stat_activity
WHERE datname IS NOT NULL
GROUP BY datname
ORDER BY connections DESC;

-- 创建表大小监控视图
CREATE OR REPLACE VIEW table_sizes AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY size_bytes DESC;

-- 创建慢查询监控视图
CREATE OR REPLACE VIEW slow_queries AS
SELECT
    substring(query, 1, 100) AS query_preview,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 50;

-- ====================================================================
-- 6. 输出初始化完成信息
-- ====================================================================
\c postgres
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '数据库初始化完成！';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建的数据库:';
    RAISE NOTICE '  1. auth_service (用户: auth_user)';
    RAISE NOTICE '  2. book_service (用户: book_user)';
    RAISE NOTICE '  3. reader_service (用户: reader_user)';
    RAISE NOTICE '  4. circulation_service (用户: circulation_user)';
    RAISE NOTICE '  5. system_service (用户: system_user)';
    RAISE NOTICE '  6. recommend_service (用户: recommend_user)';
    RAISE NOTICE '  7. nlp_service (用户: nlp_user)';
    RAISE NOTICE '  8. vision_service (用户: vision_user)';
    RAISE NOTICE '  9. analytics_service (用户: analytics_user)';
    RAISE NOTICE ' 10. notification_service (用户: notification_user)';
    RAISE NOTICE ' 11. file_service (用户: file_user)';
    RAISE NOTICE ' 12. search_service (用户: search_user)';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已安装的扩展:';
    RAISE NOTICE '  - uuid-ossp (UUID生成)';
    RAISE NOTICE '  - pg_trgm (模糊搜索)';
    RAISE NOTICE '  - btree_gin/btree_gist (全文搜索)';
    RAISE NOTICE '  - pg_stat_statements (性能监控)';
    RAISE NOTICE '========================================';
END $$;

-- 显示数据库列表
SELECT datname, pg_size_pretty(pg_database_size(datname)) as size
FROM pg_database
WHERE datname LIKE '%_service' OR datname = 'postgres'
ORDER BY datname;
