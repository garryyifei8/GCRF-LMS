#!/bin/bash
# ====================================================================
# PostgreSQL 集群健康检查脚本
# 用途: 检查主从复制状态、连接池状态、性能指标
# ====================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 数据库连接信息
PRIMARY_HOST="${PRIMARY_HOST:-localhost}"
PRIMARY_PORT="${PRIMARY_PORT:-5432}"
REPLICA1_HOST="${REPLICA1_HOST:-localhost}"
REPLICA1_PORT="${REPLICA1_PORT:-5433}"
REPLICA2_HOST="${REPLICA2_HOST:-localhost}"
REPLICA2_PORT="${REPLICA2_PORT:-5434}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-gcrf_secure_2024}"

export PGPASSWORD

# 函数: 打印标题
print_header() {
    echo ""
    echo "========================================="
    echo -e "${BLUE}$1${NC}"
    echo "========================================="
}

# 函数: 检查数据库连接
check_connection() {
    local host=$1
    local port=$2
    local name=$3

    echo -n "检查 ${name} (${host}:${port}) ... "
    if pg_isready -h "${host}" -p "${port}" -U "${PGUSER}" -q; then
        echo -e "${GREEN}✓ 在线${NC}"
        return 0
    else
        echo -e "${RED}✗ 离线${NC}"
        return 1
    fi
}

# 函数: 获取数据库版本
get_version() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -t -c "SELECT version();" 2>/dev/null | head -n 1
}

# 函数: 检查复制延迟
check_replication_lag() {
    local host=$1
    local port=$2
    local name=$3

    local lag=$(psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -t -c "
    SELECT CASE
        WHEN pg_is_in_recovery() THEN
            EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp()))
        ELSE
            0
    END;" 2>/dev/null)

    if [ -z "$lag" ]; then
        echo -e "${name}: ${RED}无法获取延迟信息${NC}"
        return 1
    fi

    local lag_int=$(printf "%.0f" "$lag")
    if [ "$lag_int" -lt 1 ]; then
        echo -e "${name}: ${GREEN}延迟 ${lag_int}s ✓${NC}"
        return 0
    elif [ "$lag_int" -lt 5 ]; then
        echo -e "${name}: ${YELLOW}延迟 ${lag_int}s ⚠${NC}"
        return 0
    else
        echo -e "${name}: ${RED}延迟 ${lag_int}s ✗${NC}"
        return 1
    fi
}

# 函数: 检查复制状态
check_replication_status() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -c "
    SELECT
        application_name,
        client_addr,
        state,
        sync_state,
        pg_wal_lsn_diff(sent_lsn, write_lsn) AS write_lag_bytes,
        pg_wal_lsn_diff(write_lsn, flush_lsn) AS flush_lag_bytes,
        pg_wal_lsn_diff(flush_lsn, replay_lsn) AS replay_lag_bytes
    FROM pg_stat_replication;" 2>/dev/null
}

# 函数: 检查数据库大小
check_database_sizes() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -c "
    SELECT
        datname AS database,
        pg_size_pretty(pg_database_size(datname)) AS size
    FROM pg_database
    WHERE datname LIKE '%_service' OR datname = 'postgres'
    ORDER BY pg_database_size(datname) DESC;" 2>/dev/null
}

# 函数: 检查连接数
check_connections() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -c "
    SELECT
        datname AS database,
        count(*) AS connections,
        max(state) AS state
    FROM pg_stat_activity
    WHERE datname IS NOT NULL
    GROUP BY datname
    ORDER BY connections DESC;" 2>/dev/null
}

# 函数: 检查缓存命中率
check_cache_hit_ratio() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -c "
    SELECT
        sum(heap_blks_read) AS heap_read,
        sum(heap_blks_hit) AS heap_hit,
        round(sum(heap_blks_hit)::numeric / nullif(sum(heap_blks_hit) + sum(heap_blks_read), 0) * 100, 2) AS cache_hit_ratio_percent
    FROM pg_statio_user_tables;" 2>/dev/null
}

# 函数: 检查慢查询
check_slow_queries() {
    local host=$1
    local port=$2

    psql -h "${host}" -p "${port}" -U "${PGUSER}" -d postgres -c "
    SELECT
        substring(query, 1, 100) AS query_preview,
        calls,
        round(total_exec_time::numeric, 2) AS total_time_ms,
        round(mean_exec_time::numeric, 2) AS mean_time_ms
    FROM pg_stat_statements
    WHERE mean_exec_time > 100
    ORDER BY mean_exec_time DESC
    LIMIT 10;" 2>/dev/null
}

# ====================================================================
# 主程序
# ====================================================================

print_header "PostgreSQL 集群健康检查"

# 1. 检查所有节点连接
print_header "1. 节点连接状态"
PRIMARY_UP=$(check_connection "${PRIMARY_HOST}" "${PRIMARY_PORT}" "主库" && echo 1 || echo 0)
REPLICA1_UP=$(check_connection "${REPLICA1_HOST}" "${REPLICA1_PORT}" "从库1" && echo 1 || echo 0)
REPLICA2_UP=$(check_connection "${REPLICA2_HOST}" "${REPLICA2_PORT}" "从库2" && echo 1 || echo 0)

# 2. 检查数据库版本
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "2. 数据库版本信息"
    echo "主库版本:"
    get_version "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 3. 检查复制延迟
print_header "3. 复制延迟检查"
if [ "${PRIMARY_UP}" -eq 1 ]; then
    if [ "${REPLICA1_UP}" -eq 1 ]; then
        check_replication_lag "${REPLICA1_HOST}" "${REPLICA1_PORT}" "从库1"
    fi
    if [ "${REPLICA2_UP}" -eq 1 ]; then
        check_replication_lag "${REPLICA2_HOST}" "${REPLICA2_PORT}" "从库2"
    fi
else
    echo -e "${RED}主库不可用，无法检查复制延迟${NC}"
fi

# 4. 检查复制状态
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "4. 主库复制状态"
    check_replication_status "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 5. 检查数据库大小
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "5. 数据库大小"
    check_database_sizes "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 6. 检查连接数
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "6. 数据库连接数"
    check_connections "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 7. 检查缓存命中率
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "7. 缓存命中率"
    check_cache_hit_ratio "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 8. 检查慢查询
if [ "${PRIMARY_UP}" -eq 1 ]; then
    print_header "8. 慢查询统计 (平均执行时间>100ms)"
    check_slow_queries "${PRIMARY_HOST}" "${PRIMARY_PORT}"
fi

# 9. 生成健康报告摘要
print_header "9. 健康检查摘要"
TOTAL_NODES=3
UP_NODES=$((PRIMARY_UP + REPLICA1_UP + REPLICA2_UP))
HEALTH_PERCENTAGE=$((UP_NODES * 100 / TOTAL_NODES))

echo "节点状态: ${UP_NODES}/${TOTAL_NODES} 在线 (${HEALTH_PERCENTAGE}%)"
echo ""

if [ "${UP_NODES}" -eq "${TOTAL_NODES}" ]; then
    echo -e "${GREEN}✓ 集群状态: 健康${NC}"
    exit 0
elif [ "${PRIMARY_UP}" -eq 1 ]; then
    echo -e "${YELLOW}⚠ 集群状态: 部分可用（主库正常）${NC}"
    exit 1
else
    echo -e "${RED}✗ 集群状态: 异常（主库不可用）${NC}"
    exit 2
fi
