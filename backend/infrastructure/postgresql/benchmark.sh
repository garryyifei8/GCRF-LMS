#!/bin/bash
# ====================================================================
# PostgreSQL 性能基准测试脚本
# 用途: 使用pgbench进行性能测试
# 目标: 写入QPS>1000, 查询QPS>5000
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 数据库连接信息
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-gcrf_secure_2024}"
PGDATABASE="${PGDATABASE:-postgres}"

# 测试参数
SCALE_FACTOR=10          # 数据规模（1=16MB，10=160MB）
CLIENTS=50               # 并发客户端数
THREADS=4                # 线程数
DURATION=60              # 测试持续时间（秒）

echo "========================================="
echo "PostgreSQL 性能基准测试"
echo "========================================="
echo "主机: ${PGHOST}:${PGPORT}"
echo "数据库: ${PGDATABASE}"
echo "并发数: ${CLIENTS}"
echo "持续时间: ${DURATION}s"
echo "========================================="

# 检查数据库连接
echo -e "${YELLOW}检查数据库连接...${NC}"
export PGPASSWORD
if ! pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}"; then
    echo -e "${RED}错误: 无法连接到数据库${NC}"
    exit 1
fi
echo -e "${GREEN}数据库连接正常${NC}"

# 初始化测试数据
echo -e "${YELLOW}初始化测试数据 (scale=${SCALE_FACTOR})...${NC}"
pgbench -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -i -s "${SCALE_FACTOR}" --quiet

# 显示表大小
echo -e "${YELLOW}测试数据规模:${NC}"
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public' AND tablename LIKE 'pgbench_%'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
"

echo ""
echo "========================================="
echo "测试1: 只读性能测试"
echo "========================================="
pgbench -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
    -c "${CLIENTS}" -j "${THREADS}" -T "${DURATION}" -S -P 5

echo ""
echo "========================================="
echo "测试2: 读写混合测试（默认）"
echo "========================================="
pgbench -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
    -c "${CLIENTS}" -j "${THREADS}" -T "${DURATION}" -P 5

echo ""
echo "========================================="
echo "测试3: 简单写入测试"
echo "========================================="
cat > /tmp/pgbench_write.sql <<EOF
INSERT INTO pgbench_history (tid, bid, aid, delta, mtime)
VALUES (random()*10, random()*10, random()*100000, random()*1000, CURRENT_TIMESTAMP);
EOF

pgbench -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
    -c "${CLIENTS}" -j "${THREADS}" -T "${DURATION}" -f /tmp/pgbench_write.sql -P 5

echo ""
echo "========================================="
echo "测试4: 复杂查询测试"
echo "========================================="
cat > /tmp/pgbench_complex.sql <<EOF
SELECT
    a.aid,
    a.abalance,
    b.bid,
    b.bbalance
FROM pgbench_accounts a
JOIN pgbench_branches b ON a.bid = b.bid
WHERE a.aid = random()*100000
LIMIT 10;
EOF

pgbench -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
    -c "${CLIENTS}" -j "${THREADS}" -T "${DURATION}" -f /tmp/pgbench_complex.sql -P 5

echo ""
echo "========================================="
echo "性能统计摘要"
echo "========================================="

# 查询慢查询统计
echo -e "${YELLOW}TOP 10 慢查询:${NC}"
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "
SELECT
    substring(query, 1, 80) AS query_preview,
    calls,
    round(total_exec_time::numeric, 2) AS total_time_ms,
    round(mean_exec_time::numeric, 2) AS mean_time_ms,
    round(max_exec_time::numeric, 2) AS max_time_ms
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY mean_exec_time DESC
LIMIT 10;
" 2>/dev/null || echo "pg_stat_statements扩展未启用"

# 查询数据库连接统计
echo ""
echo -e "${YELLOW}当前连接统计:${NC}"
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "
SELECT
    state,
    count(*) AS connections
FROM pg_stat_activity
WHERE datname = '${PGDATABASE}'
GROUP BY state
ORDER BY connections DESC;
"

# 查询缓存命中率
echo ""
echo -e "${YELLOW}缓存命中率:${NC}"
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "
SELECT
    sum(heap_blks_read) AS heap_read,
    sum(heap_blks_hit) AS heap_hit,
    round(sum(heap_blks_hit)::numeric / nullif(sum(heap_blks_hit) + sum(heap_blks_read), 0) * 100, 2) AS cache_hit_ratio
FROM pg_statio_user_tables;
"

echo ""
echo "========================================="
echo "性能测试完成"
echo "========================================="
echo -e "${GREEN}性能要求验证:${NC}"
echo "  目标写入QPS: >1000"
echo "  目标查询QPS: >5000"
echo ""
echo -e "${YELLOW}提示: 请根据上述测试结果中的 TPS (transactions per second) 值判断是否达标${NC}"
echo ""

# 清理测试数据
read -p "是否清理测试数据？(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}清理测试数据...${NC}"
    psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "
    DROP TABLE IF EXISTS pgbench_accounts CASCADE;
    DROP TABLE IF EXISTS pgbench_branches CASCADE;
    DROP TABLE IF EXISTS pgbench_history CASCADE;
    DROP TABLE IF EXISTS pgbench_tellers CASCADE;
    "
    echo -e "${GREEN}测试数据已清理${NC}"
fi

# 清理临时文件
rm -f /tmp/pgbench_write.sql /tmp/pgbench_complex.sql

echo "所有任务完成！"
