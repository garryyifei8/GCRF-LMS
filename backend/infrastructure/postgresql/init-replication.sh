#!/bin/bash
# ====================================================================
# PostgreSQL 主库复制初始化脚本
# 用途: 在主库上创建复制用户并配置复制权限
# ====================================================================

set -e

echo "========================================="
echo "初始化主库复制配置..."
echo "========================================="

# 创建复制用户
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- 创建复制用户
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = '${REPLICATION_USER:-replicator}') THEN
            CREATE USER ${REPLICATION_USER:-replicator} WITH REPLICATION ENCRYPTED PASSWORD '${REPLICATION_PASSWORD:-repl_secure_2024}';
            RAISE NOTICE '复制用户 ${REPLICATION_USER:-replicator} 创建成功';
        ELSE
            RAISE NOTICE '复制用户 ${REPLICATION_USER:-replicator} 已存在';
        END IF;
    END
    \$\$;

    -- 授予必要权限
    GRANT EXECUTE ON FUNCTION pg_catalog.pg_ls_dir(text, boolean, boolean) TO ${REPLICATION_USER:-replicator};
    GRANT EXECUTE ON FUNCTION pg_catalog.pg_stat_file(text, boolean) TO ${REPLICATION_USER:-replicator};
    GRANT EXECUTE ON FUNCTION pg_catalog.pg_read_binary_file(text) TO ${REPLICATION_USER:-replicator};
    GRANT EXECUTE ON FUNCTION pg_catalog.pg_read_binary_file(text, bigint, bigint, boolean) TO ${REPLICATION_USER:-replicator};

    -- 显示复制槽信息
    SELECT slot_name, slot_type, active, restart_lsn
    FROM pg_replication_slots;

    -- 显示WAL发送进程信息
    SELECT pid, usename, application_name, client_addr, state, sync_state
    FROM pg_stat_replication;
EOSQL

echo "========================================="
echo "主库复制配置初始化完成！"
echo "========================================="
