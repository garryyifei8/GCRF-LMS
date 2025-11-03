#!/bin/bash
# ====================================================================
# PostgreSQL 从库设置脚本
# 用途: 从主库创建基础备份并启动复制
# ====================================================================

set -e

PGDATA="/var/lib/postgresql/data"
PRIMARY_HOST="${PRIMARY_HOST:-postgres-primary}"
PRIMARY_PORT="${PRIMARY_PORT:-5432}"
REPLICATION_USER="${REPLICATION_USER:-replicator}"
REPLICATION_PASSWORD="${REPLICATION_PASSWORD:-repl_secure_2024}"

echo "========================================="
echo "设置从库复制..."
echo "主库地址: ${PRIMARY_HOST}:${PRIMARY_PORT}"
echo "========================================="

# 检查数据目录是否已初始化
if [ -f "${PGDATA}/PG_VERSION" ]; then
    echo "数据目录已存在，跳过初始化"
    exit 0
fi

# 等待主库就绪
echo "等待主库启动..."
until PGPASSWORD="${POSTGRES_PASSWORD}" psql -h "${PRIMARY_HOST}" -p "${PRIMARY_PORT}" -U "${POSTGRES_USER}" -d postgres -c '\q' 2>/dev/null; do
    echo "主库未就绪，等待2秒后重试..."
    sleep 2
done

echo "主库已就绪，开始创建基础备份..."

# 清空数据目录
rm -rf "${PGDATA}"/*

# 使用pg_basebackup创建基础备份
PGPASSWORD="${REPLICATION_PASSWORD}" pg_basebackup \
    -h "${PRIMARY_HOST}" \
    -p "${PRIMARY_PORT}" \
    -U "${REPLICATION_USER}" \
    -D "${PGDATA}" \
    -Fp \
    -Xs \
    -P \
    -R \
    -v

# 修改权限
chmod 0700 "${PGDATA}"

echo "========================================="
echo "从库基础备份创建完成！"
echo "========================================="
